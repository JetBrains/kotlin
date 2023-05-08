/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.GlobalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

fun matchIrFileWithTestFile(irModuleFragment: IrModuleFragment, module: TestModule): List<Pair<IrFile, TestFile>> {
    val irFileWithTestFile = irModuleFragment.files.map { irFile ->
        irFile to module.files.firstOrNull { testFile -> testFile.relativePath == irFile.fileEntry.name.drop(1) }
    }

    @Suppress("UNCHECKED_CAST")
    return irFileWithTestFile.filterNot { (_, testFile) -> testFile == null || testFile.isAdditional } as List<Pair<IrFile, TestFile>>
}

open class IrInterpreterBackendHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    private val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    override fun processModule(module: TestModule, info: IrBackendInput) {
        info.processAllIrModuleFragments(module) { moduleFragment, _ ->
            val evaluator = Evaluator(IrInterpreter(moduleFragment.irBuiltins), globalMetadataInfoHandler)
            for ((irFile, testFile) in matchIrFileWithTestFile(moduleFragment, module)) {
                evaluator.evaluate(irFile, testFile)
            }
        }
    }
}

private class Evaluator(private val interpreter: IrInterpreter, private val globalMetadataInfoHandler: GlobalMetadataInfoHandler) {
    fun evaluate(irFile: IrFile, testFile: TestFile) {
        object : IrElementTransformerVoid() {
            private fun IrExpression.report(original: IrExpression, startOffsetForDiagnostic: Int? = null): IrExpression {
                if (this == original) return this
                val isError = this is IrErrorExpression
                val message = when (this) {
                    is IrConst<*> -> this.value.toString()
                    is IrErrorExpression -> this.description
                    else -> TODO("unsupported type ${this::class.java}")
                }
                val startOffset = when {
                    startOffsetForDiagnostic != null -> startOffsetForDiagnostic
                    // this additional check is needed to unify rendering from old and new frontends
                    original is IrCall && original.symbol.owner.fqNameWhenAvailable?.asString() == "kotlin.internal.ir.CHECK_NOT_NULL" -> endOffset - 2
                    else -> original.startOffset
                }
                val metaInfo = IrInterpreterCodeMetaInfo(startOffset, this.endOffset, message, isError)
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, listOf(metaInfo))
                return if (this !is IrErrorExpression) this else original
            }

            override fun visitCall(expression: IrCall): IrExpression {
                // try to calculate default args of inline function at call site
                // used in `sourceLocation` test
                expression.symbol.owner.valueParameters.forEachIndexed { index, parameter ->
                    if (expression.getValueArgument(index) != null || !expression.symbol.owner.isInline) return@forEachIndexed
                    val default = parameter.defaultValue?.expression as? IrCall ?: return@forEachIndexed
                    val callWithNewOffsets = IrCallImpl(
                        expression.startOffset, expression.endOffset, default.type, default.symbol,
                        default.typeArgumentsCount, default.valueArgumentsCount, default.origin, default.superQualifierSymbol
                    )
                    callWithNewOffsets.copyTypeAndValueArgumentsFrom(default)
                    interpreter.interpret(callWithNewOffsets, irFile)
                        .report(callWithNewOffsets)
                        .takeIf { it != callWithNewOffsets }
                        ?.apply { expression.putArgument(parameter, this) }
                }
                return super.visitCall(expression)
            }

            override fun visitField(declaration: IrField): IrStatement {
                val initializer = declaration.initializer
                val expression = initializer?.expression ?: return declaration
                if (expression is IrConst<*>) return declaration

                val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
                if (isConst) {
                    val startOffsetForDiagnostic = declaration.startOffset + "const val  = ".length + declaration.name.asString().length
                    initializer.expression = interpreter.interpret(expression, irFile).report(expression, startOffsetForDiagnostic)
                }
                return declaration
            }
        }.visitFile(irFile)
    }
}

