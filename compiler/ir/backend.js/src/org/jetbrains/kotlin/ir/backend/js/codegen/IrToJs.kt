/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.codegen

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.LoweredIr
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity.*
import org.jetbrains.kotlin.ir.backend.js.export.*
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrFileToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.processClassModels
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasInterfaceParent
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import kotlin.math.abs

interface CompilerOutputSink {
    fun write(module: String, path: String, content: String)
}

class JsGenerationOptions(
    val jsExtension: String = "js",
    val generatePackageJson: Boolean = false,
    val generateTypeScriptDefinitions: Boolean = false,
)

class IrToJs(
    private val backendContext: JsIrBackendContext,
    private val guid: (IrDeclaration) -> String,
    private val outputSink: CompilerOutputSink,
    private val mainArguments: List<String>?,
    private val granularity: JsGenerationGranularity,
    private val mainModuleName: String,
    private val options: JsGenerationOptions,
) {
    val indexFileName = "index.${options.jsExtension}"

    val FileUnit.initFunctionName
        get() = "KotlinInit$" + sanitizeName(pathToJsModule(file))

    sealed class CodegenUnitReference
    object ThisUnitReference : CodegenUnitReference()
    inner class OtherUnitReference(
        module: IrModuleFragment,
    ) : CodegenUnitReference() {
        // Path to entry point of other module from "top-level", e.g. directory which contains all other modules
        val importPath = "./" + module.jsModuleName + "/" + indexFileName
    }

    abstract class CodegenUnit {
        abstract val packageFragments: Iterable<IrPackageFragment>
        abstract val externalPackageFragments: Iterable<IrPackageFragment>
        abstract fun referenceCodegenUnitOfDeclaration(declaration: IrDeclaration): CodegenUnitReference
        abstract val pathToKotlinModulesRoot: String
    }

    inner class FileUnit(val file: IrFile, val externalFile: IrFile?) : CodegenUnit() {
        override val packageFragments =
            listOf(file)

        override val externalPackageFragments =
            listOfNotNull(externalFile)

        override fun referenceCodegenUnitOfDeclaration(declaration: IrDeclaration): CodegenUnitReference =
            when (val declarationFile = declaration.file) {
                file -> ThisUnitReference
                else -> OtherUnitReference(declarationFile.module)
            }

        override val pathToKotlinModulesRoot: String by lazy {
            "../".repeat(file.fqName.pathSegments().size + 1)
        }
    }

    inner class ModuleUnit(val module: IrModuleFragment) : CodegenUnit() {
        override val packageFragments: Iterable<IrPackageFragment> =
            module.files

        override val externalPackageFragments: Iterable<IrPackageFragment> =
            packageFragments.mapNotNull { backendContext.externalPackageFragment[it.symbol] }

        override fun referenceCodegenUnitOfDeclaration(declaration: IrDeclaration): CodegenUnitReference =
            when (val declarationModule = declaration.file.module) {
                module -> ThisUnitReference
                else -> OtherUnitReference(declarationModule)
            }

        override val pathToKotlinModulesRoot: String = "../"
    }

    class WholeProgramUnit(
        val modules: Iterable<IrModuleFragment>,
        val externalModules: Iterable<IrPackageFragment>
    ) : CodegenUnit() {
        override val packageFragments: Iterable<IrPackageFragment> =
            modules.flatMap { it.files }

        override val externalPackageFragments: Iterable<IrPackageFragment>
            get() = externalModules

        override fun referenceCodegenUnitOfDeclaration(declaration: IrDeclaration): CodegenUnitReference =
            ThisUnitReference

        override val pathToKotlinModulesRoot: String
            get() = "../"
    }

    private fun pathToJsModule(file: IrFile): String =
        "${fileJsRootModuleName(file)}/${fileJsSubModulePath(file)}"

    private fun fileJsRootModuleName(file: IrFile): String =
        when (granularity) {
            WHOLE_PROGRAM -> mainModuleName
            PER_MODULE, PER_FILE -> file.module.jsModuleName
        }

    private fun fileJsSubModulePath(file: IrFile): String =
        when (granularity) {
            WHOLE_PROGRAM, PER_MODULE -> indexFileName

            PER_FILE -> {
                val maybeSingleOpenClass = (file.declarations.singleOrNull() as? IrClass)?.takeIf {
                    it.modality == Modality.ABSTRACT || it.modality == Modality.OPEN
                }

                val hash = abs((maybeSingleOpenClass?.let { guid(it) } ?: file.path).hashCode())
                val filePrefix = maybeSingleOpenClass?.name?.asString()?.let { sanitizeName(it) + ".class" } ?: file.name
                val fileName = "${filePrefix}_$hash.${options.jsExtension}"
                val packagePath = file.fqName.pathSegments().joinToString("") { it.identifier + "/" }
                "$packagePath$fileName"
            }
        }

    class GeneratedUnit(
        val jsStatements: List<JsStatement>,
        val exportedDeclarations: List<ExportedDeclaration>,
    )

    fun generateUnit(unit: CodegenUnit): GeneratedUnit {
        val exportedDeclarations: List<ExportedDeclaration> =
            with(ExportModelGenerator(backendContext, generateNamespacesForPackages = false)) {
                (unit.externalPackageFragments + unit.packageFragments).flatMap { packageFragment ->
                    generateExport(packageFragment)
                }
            }

        val stableNames: Set<String> = collectStableNames(unit)
        val nameGenerator = NewNamerImpl(backendContext, unit, guid, stableNames)

        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator,
            globalNameScope = nameGenerator.staticNames
        )

        val declarationStatements: List<JsStatement> = unit.packageFragments.flatMap {
            StaticMembersLowering(backendContext).lower(it as IrFile)
            it.accept(IrFileToJsTransformer(), staticContext).statements
        }

        val preDeclarationBlock = JsGlobalBlock()
        val postDeclarationBlock = JsGlobalBlock()
        processClassModels(staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        val statements = mutableListOf<JsStatement>()
        statements += nameGenerator.internalImports.values
        statements += preDeclarationBlock
        statements += declarationStatements
        statements += postDeclarationBlock

        // Generate module initialization

        val initializerBlock = staticContext.initializerBlock
        when (unit) {
            is WholeProgramUnit, is ModuleUnit -> {
                // Run initialization during ES module initialization
                statements += initializerBlock
            }

            is FileUnit -> {
                // Postpone initialization by putting it into a separate function
                // Will be called later in proper order after class model is initialized
                val initFunction = JsFunction(emptyScope, JsBlock(initializerBlock.statements), "init fun")
                initFunction.name = JsName(unit.initFunctionName, false)
                statements += initFunction.makeStmt()
                statements += JsExport(initFunction.name)
            }
        }

        // Generate internal export

        val internalExports = mutableListOf<JsExport.Element>()
        fun export(declaration: IrDeclarationWithName) {
            internalExports += JsExport.Element(nameGenerator.getNameForStaticDeclaration(declaration), JsName(guid(declaration), false))
        }

        for (fragment in unit.packageFragments) {
            for (declaration in fragment.declarations) {
                if (declaration is IrDeclarationWithName) {
                    export(declaration)
                }

                // Default implementations of interface methods are nested under interface declarations in IR at this point,
                // but they are effectively used as a static declaration and can be directly referenced by other codegen unit,
                // thus requiring internal export
                declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                        if (declaration.hasInterfaceParent() && declaration.body != null) {
                            export(declaration)
                        }
                        super.visitSimpleFunction(declaration)
                    }
                })
            }
        }
        statements += JsExport(JsExport.Subject.Elements(internalExports), null)

        // Generate external export

        val globalNames = NameTable<String>(nameGenerator.staticNames)
        val exporter = ExportModelToJsStatements(
            nameGenerator,
            declareNewNamespace = { globalNames.declareFreshName(it, it) }
        )
        exportedDeclarations.forEach {
            statements += exporter.generateDeclarationExport(
                it,
                null,
                esModules = true
            )
        }

        return GeneratedUnit(statements, exportedDeclarations)
    }

    private fun collectStableNames(unit: CodegenUnit): Set<String> {
        val newStableStaticNamesCollectorVisitor =
            NewStableStaticNamesCollectorVisitor(needToCollectReferences = granularity != WHOLE_PROGRAM)
        unit.packageFragments.forEach { it.acceptVoid(newStableStaticNamesCollectorVisitor) }
        unit.externalPackageFragments.forEach { it.acceptVoid(newStableStaticNamesCollectorVisitor) }

        return newStableStaticNamesCollectorVisitor.collectedStableNames
    }

    // Returns import statement and call expression
    private fun invokeFunctionFromEntryJsFile(
        function: IrFunction,
        args: List<JsExpression> = emptyList()
    ): Pair<JsStatement, JsExpression> {
        val name = guid(function)
        val importPath = if (granularity == WHOLE_PROGRAM) "./$indexFileName" else "../" + pathToJsModule(function.file)
        return Pair(
            JsImport(importPath, mutableListOf(JsImport.Element(name, null))),
            JsInvocation(JsNameRef(name), args)
        )
    }

    private fun invokeFunctionFromEntryJsFileAsStatements(
        function: IrFunction,
        args: List<JsExpression> = emptyList()
    ): List<JsStatement> =
        invokeFunctionFromEntryJsFile(function, args)
            .let { listOf(it.first, it.second.makeStmt()) }

    fun generateModules(
        mainModule: IrModuleFragment,
        allModules: List<IrModuleFragment>
    ) {
        when (granularity) {
            WHOLE_PROGRAM ->
                generateModule(mainModule, allModules)

            PER_MODULE,
            PER_FILE ->
                allModules.forEach { module ->
                    generateModule(mainModule = module, allModules = emptyList())
                }
        }
    }

    fun generateModuleLevelCode(module: IrModuleFragment, statements: MutableList<JsStatement>) {
        if (mainArguments != null) {
            val mainFunction = JsMainFunctionDetector(backendContext).getMainFunctionOrNull(module)
            if (mainFunction != null) {
                val generateArgv = mainFunction.valueParameters.firstOrNull()?.isStringArrayParameter() ?: false
                val generateContinuation = mainFunction.isLoweredSuspendFunction(backendContext)

                val mainArgumentsArray =
                    if (generateArgv)
                        JsArrayLiteral(mainArguments.map { JsStringLiteral(it) })
                    else
                        null

                val continuation =
                    if (generateContinuation) {
                        val (import, invoke) = invokeFunctionFromEntryJsFile(backendContext.coroutineEmptyContinuation.owner.getter!!)
                        statements += import
                        invoke
                    } else
                        null

                statements += invokeFunctionFromEntryJsFileAsStatements(
                    mainFunction, listOfNotNull(mainArgumentsArray, continuation)
                )
            }
        }

        // TODO: tests
//        backendContext.testRoots[module]?.let { testContainer ->
//            statements += invokeFunctionFromEntryJsFileAsStatements(testContainer)
//        }
    }

    fun generateModule(
        mainModule: IrModuleFragment,
        allModules: List<IrModuleFragment>,
    ) {
        val moduleName = mainModule.jsModuleName
        val indexJsStatements = mutableListOf<JsStatement>()
        val exportedDeclarations = mutableListOf<ExportedDeclaration>()

        when (granularity) {
            PER_FILE -> {
                for (file in mainModule.files.sortedBy(::fileInitOrder)) {
                    if (file.declarations.isEmpty()) continue

                    val pathToSubModule = fileJsSubModulePath(file)
                    indexJsStatements += JsExport(JsExport.Subject.All, fromModule = "./$pathToSubModule")

                    val unit = FileUnit(file, backendContext.externalPackageFragment[file.symbol])
                    val generatedUnit = generateUnit(unit)

                    val importElements = JsImport.Element(unit.initFunctionName, null)
                    indexJsStatements += JsImport("./$pathToSubModule", mutableListOf(importElements))
                    indexJsStatements += JsInvocation(JsNameRef(JsName(unit.initFunctionName, false))).makeStmt()

                    exportedDeclarations += generatedUnit.exportedDeclarations

                    outputSink.write(
                        file.module.jsModuleName,
                        pathToSubModule,
                        "// Kotlin file: ${file.path}\n" + generatedUnit.jsStatements.toJsCodeString()
                    )
                }
                generateModuleLevelCode(mainModule, indexJsStatements)
            }

            PER_MODULE -> {
                val generatedUnit = generateUnit(ModuleUnit(mainModule))
                indexJsStatements += generatedUnit.jsStatements
                generateModuleLevelCode(mainModule, indexJsStatements)
                exportedDeclarations += generatedUnit.exportedDeclarations
            }

            WHOLE_PROGRAM -> {
                val generatedUnit = generateUnit(WholeProgramUnit(allModules, backendContext.externalPackageFragment.values))
                indexJsStatements += generatedUnit.jsStatements
                allModules.forEach {
                    generateModuleLevelCode(it, indexJsStatements)
                }
                exportedDeclarations += generatedUnit.exportedDeclarations
            }
        }

        outputSink.write(moduleName, indexFileName, indexJsStatements.toJsCodeString())

        if (options.generatePackageJson) {
            outputSink.write(moduleName, "package.json", """{ "main": "$indexFileName", "type": "module" }""")
        }

        if (options.generateTypeScriptDefinitions && exportedDeclarations.isNotEmpty()) {
            val dts = ExportedModule(moduleName, moduleKind = ModuleKind.ES, exportedDeclarations).toTypeScript()
            outputSink.write(moduleName, "index.d.ts", dts)
        }
    }

    private fun fileInitOrder(file: IrFile): Int =
        when (val singleDeclaration = file.declarations.singleOrNull()) {
            // Initialize parent classes before child classes
            //  TODO: Comment about open classes in separate files
            is IrClass -> singleDeclaration.getInheritanceChainLength()
            // Initialize regular files after all open classes
            else -> Int.MAX_VALUE
        }

    private fun IrClass.getInheritanceChainLength(): Int {
        if (symbol == backendContext.irBuiltIns.anyClass)
            return 0

        // FIXME: Filter out interfaces
        superTypes.forEach { superType ->
            val superClass: IrClass? = superType.classOrNull?.owner
            if (superClass != null && /* !!! */ !superClass.isInterface)
                return superClass.getInheritanceChainLength() + 1
        }


        return 1
    }
}

private val IrModuleFragment.jsModuleName: String
    get() = name.asString().dropWhile { it == '<' }.dropLastWhile { it == '>' }

private fun List<JsStatement>.toJsCodeString(): String =
    JsGlobalBlock().also { it.statements += this }.toString()

enum class JsGenerationGranularity {
    WHOLE_PROGRAM,
    PER_MODULE,
    PER_FILE
}

fun generateEsModules(
    ir: LoweredIr,
    outputSink: CompilerOutputSink,
    mainArguments: List<String>?,
    granularity: JsGenerationGranularity,
    options: JsGenerationOptions,
) {
    // Declaration numeration to create temporary GUID
    // TODO: Replace with an actual GUID
    val numerator = StaticDeclarationNumerator()
    ir.allModules.forEach { numerator.add(it) }

    fun guid(declaration: IrDeclaration): String {
        val name = sanitizeName((declaration as IrDeclarationWithName).name.toString())
        val number = numerator.numeration[declaration]
            ?: error("Can't find number for declaration ${declaration.fqNameWhenAvailable}")
        // TODO: Use shorter names in release mode
        return "${name}_GUID_${number}"
    }

    val ir2js = IrToJs(ir.context, ::guid, outputSink, mainArguments, granularity, ir.mainModule.jsModuleName, options)
    ir2js.generateModules(ir.mainModule, ir.allModules)
}
