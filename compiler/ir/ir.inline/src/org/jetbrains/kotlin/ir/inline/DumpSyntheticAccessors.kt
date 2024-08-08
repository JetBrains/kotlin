/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.syntheticBodyIsNotSupported
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.io.File

class DumpSyntheticAccessors(context: CommonBackendContext) : ModuleLoweringPass {
    private val dumpDirectory: File? = getDumpDirectoryOrNull(context.configuration)

    override fun lower(irModule: IrModuleFragment) {
        val dumpDirectory = dumpDirectory ?: return // skip if there is no dump directory
        dumpDirectory.mkdirs()

        val fileDumps = HashMap<FileKey, HashSet<String>>()

        for (irFile in irModule.files) {
            val fileDump = dumpFile(irFile)
            if (fileDump != null) {
                // Keep "duplicated" dumps for the case when there are two files with the same name and package FQN.
                fileDumps.computeIfAbsent(FileKey(irFile)) { HashSet() } += fileDump
            }
        }

        if (fileDumps.isNotEmpty() && fileDumps.values.any(Collection<*>::isNotEmpty)) {
            getDumpFileForModule(dumpDirectory, irModule.name).printWriter().use { writer ->
                writer.appendLine("/* MODULE name=${irModule.name.asString()} */")
                writer.appendLine()

                fileDumps.entries.sortedBy { it.key }.forEach { (fileKey, dumps) ->
                    if (dumps.isNotEmpty()) {
                        writer.appendLine("/* FILE package=${fileKey.packageFqName.ifEmpty { "<root>" }} fileName=${fileKey.fileName} */")
                        writer.appendLine()
                        for (fileDump in dumps.sorted()) {
                            writer.appendLine(fileDump)
                        }
                        writer.appendLine()
                    }
                }
            }
        }
    }

    private fun dumpFile(irFile: IrFile): String? {
        val accessorSymbols = SyntheticAccessorCollector().also(irFile::acceptVoid).accessorSymbols
        if (accessorSymbols.isEmpty()) return null

        val accessorTargetSymbols = collectAccessorTargetSymbols(accessorSymbols)

        return SyntheticAccessorsDumper(accessorSymbols, accessorTargetSymbols).also(irFile::acceptVoid).getDump()
    }

    private fun collectAccessorTargetSymbols(accessorSymbols: Set<IrFunctionSymbol>): Set<IrSymbol> =
        accessorSymbols.mapToSetOrEmpty { accessorSymbol ->
            when (val accessor = accessorSymbol.owner) {
                is IrConstructor -> {
                    val accessorTargetSymbol: IrConstructorSymbol = accessor.getSingleExpression<IrDelegatingConstructorCall>().symbol
                    accessorTargetSymbol
                }
                is IrSimpleFunction -> when (val expression = accessor.getSingleExpression<IrDeclarationReference>()) {
                    is IrCall -> {
                        val accessorTargetSymbol: IrSimpleFunctionSymbol = expression.symbol
                        accessorTargetSymbol
                    }
                    is IrFieldAccessExpression -> {
                        val accessorTargetSymbol: IrFieldSymbol = expression.symbol
                        accessorTargetSymbol
                    }
                    is IrValueAccessExpression -> {
                        val accessorTargetSymbol: IrValueSymbol = expression.symbol
                        accessorTargetSymbol
                    }
                    is IrConstructorCall -> {
                        val accessorTargetSymbol: IrConstructorSymbol = accessor.getSingleExpression<IrConstructorCall>().symbol
                        accessorTargetSymbol
                    }
                    else -> error("Unexpected type of expression in accessor ${accessor.id()}, ${expression.render()}")
                }
                else -> error("Unexpected accessor type: ${accessor.id()}")
            }
        }

    private inline fun <reified E : IrExpression> IrFunction.getSingleExpression(): E {
        val body = body ?: compilationException("${id()} has no body", this)
        val expression = when (body) {
            is IrExpressionBody -> body.expression
            is IrBlockBody -> body.statements.singleOrNull() as? IrExpression
                ?: compilationException("${id()} is expected to have exactly the single expression in block body", this)
            is IrSyntheticBody -> syntheticBodyIsNotSupported(this)
        }

        if (expression !is E)
            compilationException(
                "${id()} is expected to have expression of type ${E::class.simpleName} but has ${expression::class.simpleName}",
                this,
            )

        return expression
    }

    private fun IrFunction.id(): String = "${this::class.java.simpleName} $fqNameWhenAvailable"

    private data class FileKey(val packageFqName: String, val fileName: String) : Comparable<FileKey> {
        constructor(irFile: IrFile) : this(irFile.packageFqName.asString(), irFile.name)

        override fun compareTo(other: FileKey) = compareValuesBy(this, other, FileKey::packageFqName, FileKey::fileName)
    }

    companion object {
        fun getDumpDirectoryOrNull(configuration: CompilerConfiguration): File? =
            configuration[KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR]?.let(::File)

        fun getDumpFileForModule(dumpDirectory: File, moduleName: Name): File =
            dumpDirectory.resolve("synthetic-accessors-dump-${moduleName.asStringStripSpecialMarkers()}.kt")

    }
}

private class SyntheticAccessorCollector : IrElementVisitorVoid {
    val accessorSymbols = HashSet<IrFunctionSymbol>()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        runIf(declaration.origin == IrDeclarationOrigin.SYNTHETIC_ACCESSOR) { accessorSymbols += declaration.symbol }
        super.visitFunction(declaration)
    }
}

private class SyntheticAccessorsDumper(
    private val accessorSymbols: Set<IrFunctionSymbol>,
    private val accessorTargetSymbols: Set<IrSymbol>
) : IrElementVisitorVoid {
    private val stack = ArrayList<StackFrame>()
    private val dump = StringBuilder()

    fun getDump(): String? = dump.toString().takeIf(String::isNotBlank)

    private inline fun <T> withNewStackFrame(declaration: IrDeclaration, crossinline block: () -> T): T =
        stack.temporarilyPushing(StackFrame(declaration)) { block() }

    private fun dumpCurrentStackIfSymbolIsObserved(symbol: IrSymbol) {
        if (symbol in accessorSymbols || symbol in accessorTargetSymbols) {
            for ((index, stackFrame) in stack.withIndex()) {
                stackFrame.ifNotYetPrinted { declaration ->
                    val comment = when (declaration.symbol) {
                        in accessorSymbols -> "/* ACCESSOR declaration */ "
                        in accessorTargetSymbols -> "/* TARGET declaration */ "
                        else -> ""
                    }

                    dump.appendIndent(indent = index).append(comment).appendIrElement(declaration)
                }
            }
        }
    }

    private fun dumpExpressionIfSymbolIsObserved(expression: IrDeclarationReference) {
        val comment = when (expression.symbol) {
            in accessorSymbols -> "/* ACCESSOR use-site */ "
            in accessorTargetSymbols -> "/* TARGET use-site */ "
            else -> return
        }

        dump.appendIndent(indent = stack.size).append(comment).appendIrElement(expression)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)

        // IR declaration reference expression can be a call site of an accessor or an accessor target.
        // Dump the current stack if either of.
        dumpCurrentStackIfSymbolIsObserved(expression.symbol)
        dumpExpressionIfSymbolIsObserved(expression)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) = withNewStackFrame(declaration) {
        // IR declaration can an accessor, or an accessor target.
        // Dump the current stack if either of.
        dumpCurrentStackIfSymbolIsObserved(declaration.symbol)

        super.visitDeclaration(declaration)
    }

    private class StackFrame(private val declaration: IrDeclaration) {
        private var printed: Boolean = false

        inline fun ifNotYetPrinted(block: (IrDeclaration) -> Unit) {
            if (!printed) {
                block(declaration)
                printed = true
            }
        }
    }

    companion object {
        private fun StringBuilder.appendIrElement(element: IrElement): StringBuilder {
            val dump = element.dumpKotlinLike(
                KotlinLikeDumpOptions(
                    customDumpStrategy = object : CustomKotlinLikeDumpStrategy {
                        override fun shouldPrintAnnotation(annotation: IrConstructorCall, container: IrAnnotationContainer) = false
                    },
                    printFakeOverridesStrategy = FakeOverridesStrategy.NONE,
                    bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES,
                    visibilityPrintingStrategy = VisibilityPrintingStrategy.ALWAYS,
                    printMemberDeclarations = false
                )
            ).substringBefore('{').trimEnd()

            return appendLine(dump)
        }

        private fun StringBuilder.appendIndent(indent: Int): StringBuilder {
            repeat(indent) { append("    ") }
            return this
        }
    }
}
