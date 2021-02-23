/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.codegen

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrFileToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.processClassModels
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import kotlin.math.abs

interface JsFileWriter {
    fun write(module: String, path: String, content: String)
}

class IrToJs(
    private val backendContext: JsIrBackendContext,
    private val guid: (IrDeclaration) -> String,
    private val fileWriter: JsFileWriter
) {
    sealed class DeclarationUnitReference {
        object ThisUnit : DeclarationUnitReference()
        data class OtherUnit(val importId: String) : DeclarationUnitReference()
    }

    abstract class CodegenUnit {
        abstract val packageFragments: Iterable<IrPackageFragment>
        abstract val externalFragments: Iterable<IrPackageFragment>
        abstract val topLevelDeclarations: Iterable<IrDeclaration>
        abstract fun getDeclarationUnitReference(declaration: IrDeclaration): DeclarationUnitReference
    }

    class WholeProgramUnit(
        val modules: Iterable<IrModuleFragment>,
    ) : CodegenUnit() {
        override val packageFragments: Iterable<IrPackageFragment> =
            modules.flatMap { it.files }

        override val externalFragments: Iterable<IrPackageFragment>
            get() = TODO()

        override val topLevelDeclarations: Iterable<IrDeclaration> =
            packageFragments.flatMap { it.declarations }

        override fun getDeclarationUnitReference(declaration: IrDeclaration): DeclarationUnitReference =
            DeclarationUnitReference.ThisUnit
    }

    private fun jsModuleName(module: IrModuleFragment): String =
        module.name.asString().dropWhile { it == '<' }.dropLastWhile { it == '>' }

    fun fileModuleName(file: IrFile): String {
        val singleClass = file.declarations.singleOrNull() as? IrClass
        val hash = abs((singleClass?.let { guid(it) } ?: file.path).hashCode())
        val fileName = (singleClass?.name?.identifier?.let { "$it.class" } ?: file.name) + "_$hash.js"
        val packageName = file.fqName.pathSegments().joinToString("") { it.identifier + "/" }
        val moduleName = jsModuleName(file.module)
        return "$moduleName/$packageName$fileName"
    }

    inner class FileUnit(val file: IrFile, val externalFile: IrFile?) : CodegenUnit() {
        override val packageFragments: Iterable<IrPackageFragment> = listOf(file)

        override val topLevelDeclarations: Iterable<IrDeclaration> =
            file.declarations

        override val externalFragments: Iterable<IrPackageFragment> =
            listOfNotNull(externalFile)

        override fun getDeclarationUnitReference(declaration: IrDeclaration): DeclarationUnitReference {
            require(!declaration.isEffectivelyExternal()) { "Can't reference external declaration" }

            val declFile = declaration.file
            if (declFile == file) {
                return DeclarationUnitReference.ThisUnit
            }

            val pathToTopLevel = buildString {
                repeat(file.fqName.pathSegments().size + 1) { append("../") }
            }

            return DeclarationUnitReference.OtherUnit(pathToTopLevel + jsModuleName(declFile.module) + "/index.js")
        }

        override fun toString(): String {
            return "file unit: ${fileModuleName(file)}"
        }
    }

    class GeneratedUnit(
        val jsModule: String,
        val jsExportPart: String? = null,
        val globalExportedDeclarations: List<ExportedDeclaration>,
    )

    fun generateUnit(unit: CodegenUnit): GeneratedUnit {
        val globalExportedDeclarations: List<ExportedDeclaration> =
            with(ExportModelGenerator(backendContext)) {
                (unit.externalFragments + unit.packageFragments).flatMap { packageFragment ->
                    generateExport(packageFragment)
                }
            }

        // -- Collect stable names.
        val newStableStaticNamesCollectorVisitor =
            NewStableStaticNamesCollectorVisitor(needToCollectReferences = true)
        unit.topLevelDeclarations.forEach { it.acceptVoid(newStableStaticNamesCollectorVisitor) }
        val stableNames: Set<String> = newStableStaticNamesCollectorVisitor.collectedStableNames

        // -- Generate names.
        val namer = NewNamerImpl(backendContext, unit, exportId = guid, stableNames)

        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = namer,
            globalNameScope = namer.staticNames
        )

        val rootContext = JsGenerationContext(
            currentFunction = null,
            staticContext = staticContext,
            localNames = LocalNameGenerator(NameTable())
        )

        val fileStatements: MutableList<JsStatement> = unit.packageFragments.flatMap {
            it.accept(IrFileToJsTransformer(), rootContext).statements
        }.toMutableList()

        fileStatements += rootContext.staticContext.initializerBlock.statements

        val preDeclarationBlock = JsGlobalBlock()
        val postDeclarationBlock = JsGlobalBlock()

        processClassModels(rootContext.staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        val gb = JsGlobalBlock()
        gb.statements += JsSingleLineComment("Internal imports")
        gb.statements += namer.internalImports.values
        gb.statements += JsSingleLineComment("Body")
        gb.statements += preDeclarationBlock.statements
        gb.statements += fileStatements
        gb.statements += postDeclarationBlock

        val internalExports = mutableListOf<JsExport.Element>()

        fun export(declaration: IrDeclarationWithName) {
            internalExports += JsExport.Element(namer.getNameForStaticDeclaration(declaration), JsName(guid(declaration)))
        }

        // -- Generate internal
        for (declaration in unit.topLevelDeclarations) {
            if (declaration is IrDeclarationWithName) {
                export(declaration)
            }
            declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    val parent = declaration.parent as? IrClass
                    if ((parent?.isInterface == true) && declaration.body != null) {
                        export(declaration)
                    }

                    super.visitSimpleFunction(declaration)
                }
            })
        }

        gb.statements += JsExport(JsExport.Subject.Elements(internalExports), null)

        val globalNames = NameTable<String>(namer.staticNames)
        val exporter = ExportModelToJsStatements(namer) { globalNames.declareFreshName(it, it) }
        globalExportedDeclarations.forEach {
            gb.statements += exporter.generateDeclarationExport(it, null)
        }

        return GeneratedUnit(gb.toString(), null, globalExportedDeclarations)
    }


    private fun jsInvoke(function: IrFunction, args: List<JsExpression> = emptyList()): Pair<JsStatement, JsExpression> {
        val name = guid(function)
        val el = JsImport.Element(name, null)
        val fileName = "../" + fileModuleName(function.file)

        return Pair(
            JsImport(fileName, listOf(el).toMutableList()),
            JsInvocation(JsNameRef(name), args)
        )
    }

    private fun jsInvokeStmts(function: IrFunction, args: List<JsExpression> = emptyList()): List<JsStatement> =
        jsInvoke(function, args).let { listOf(it.first, it.second.makeStmt()) }

    fun generateModule(module: IrModuleFragment, mainArguments: List<String>? = emptyList()) {
        val moduleName = jsModuleName(module)
        val sortedIrFiles = module.files.sortedBy(::fileInitOrder)
        val moduleInitBlock = JsGlobalBlock()

        val globalExportedDeclarations = mutableListOf<ExportedDeclaration>()

        for (file in sortedIrFiles) {
            if (file.declarations.isEmpty()) continue
            val fileName = fileModuleName(file)

            moduleInitBlock.statements += JsExport(JsExport.Subject.All, fromModule = "../$fileName")

//            // Move external declarations back to the file
//            backendContext.externalPackageFragment[file.symbol]?.declarations?.forEach { extDecl ->
//                file.declarations += extDecl
//                extDecl.parent = file
//            }


            val unit = generateUnit(FileUnit(file, backendContext.externalPackageFragment[file.symbol]))
            globalExportedDeclarations += unit.globalExportedDeclarations
            fileWriter.write(".", fileName, "// FILE: " + file.path + "\n" + unit.jsModule)
        }

        if (mainArguments != null) {
            val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(module)
            if (mainFunction != null) {

                val mainArgumentsArray =
                    if (mainFunction.valueParameters.isNotEmpty())
                        JsArrayLiteral(mainArguments.map { JsStringLiteral(it) })
                    else
                        null

                val statements: MutableList<JsStatement> = mutableListOf()
                val continuation =
                    if (mainFunction.isSuspend) {
                        val (import, invoke) = jsInvoke(backendContext.coroutineEmptyContinuation.owner.getter!!)
                        statements += import
                        invoke
                    } else
                        null

                moduleInitBlock.statements += statements
                moduleInitBlock.statements += jsInvokeStmts(
                    mainFunction, listOfNotNull(mainArgumentsArray, continuation)
                )
            }
        }

        backendContext.testRoots[module]?.let { testContainer ->
            moduleInitBlock.statements += jsInvokeStmts(testContainer)
        }

        // TODO: Don't create dir if module is empty
        fileWriter.write(moduleName, "index.js", moduleInitBlock.toString())
        fileWriter.write(moduleName, "package.json", """{ "main": "index.js" }""")


        // TODO: Only generate d.ts under flag
        if (globalExportedDeclarations.isNotEmpty()) {
            val dts = ExportedModule(moduleName, moduleKind = ModuleKind.COMMON_JS, globalExportedDeclarations).toTypeScript()
            fileWriter.write(moduleName, "index.d.ts", dts)
        }
    }

    private fun fileInitOrder(file: IrFile): Int =
        when (val singleDeclaration = file.declarations.singleOrNull()) {
            // Initialize parent classes before child classes
            is IrClass -> singleDeclaration.getInheritanceChainLength()
            // Initialize regular files after all open classes
            else -> Int.MAX_VALUE
        }

    private fun IrClass.getInheritanceChainLength(): Int {
        if (symbol == backendContext.irBuiltIns.anyClass)
            return 0

        superTypes.forEach { superType ->
            val superClass: IrClass? = superType.classOrNull?.owner
            if (superClass != null)
                return superClass.getInheritanceChainLength() + 1
        }

        error("Class missing super class $fqNameWhenAvailable")
    }
}

enum class JsGenerationGranularity {
    WHOLE_PROGRAM,
    PER_MODULE,
    PER_FILE
}

fun generateEsModules(
    modules: List<IrModuleFragment>,
    fileWriter: JsFileWriter,
    context: JsIrBackendContext,
    mainArguments: List<String>?,
    granularity: JsGenerationGranularity
) {
    // Declaration numeration to create temporary GUID
    // TODO: Replace with an actual GUID
    val numerator = StaticDeclarationNumerator()
    modules.forEach { numerator.add(it) }

    assert(granularity == JsGenerationGranularity.PER_FILE)

    fun guid(declaration: IrDeclaration): String {
        val tempName = if (declaration is IrDeclarationWithName) {
            sanitizeName(declaration.fqNameWhenAvailable.toString())
        } else error("")

        return tempName + "_GUID_" +
                (numerator.numeration[declaration]
                    ?: error(tempName))
    }

    val ir2js = IrToJs(context, ::guid, fileWriter)
    for (module in modules) {
        ir2js.generateModule(module, mainArguments = mainArguments)
    }
}




