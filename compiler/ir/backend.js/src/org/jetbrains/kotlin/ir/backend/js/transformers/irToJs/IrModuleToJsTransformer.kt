/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelGenerator
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelToJsStatements
import org.jetbrains.kotlin.ir.backend.js.export.ExportedModule
import org.jetbrains.kotlin.ir.backend.js.export.toTypeScript
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.NoOpSourceLocationConsumer
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.util.*

class IrModuleToJsTransformer(
    private val backendContext: JsIrBackendContext,
    private val mainArguments: List<String>?,
    private val generateScriptModule: Boolean = false,
    var namer: NameTables = NameTables(emptyList(), context = backendContext),
    private val fullJs: Boolean = true,
    private val dceJs: Boolean = false,
    private val multiModule: Boolean = false,
    private val relativeRequirePath: Boolean = false,
    private val moduleToName: Map<IrModuleFragment, String> = emptyMap(),
    private val removeUnusedAssociatedObjects: Boolean = true
) {
    private val generateRegionComments = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)

    fun generateModule(modules: Iterable<IrModuleFragment>): CompilerResult {
        val additionalPackages = with(backendContext) {
            externalPackageFragment.values + listOf(
                bodilessBuiltInsPackageFragment,
            ) + packageLevelJsModules
        }

        val moduleKind: ModuleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!
        val exportedModule = ExportModelGenerator(backendContext, generateNamespacesForPackages = true).generateExport(modules, moduleKind = moduleKind)
        val dts = exportedModule.toTypeScript()

        modules.forEach { module ->
            module.files.forEach { StaticMembersLowering(backendContext).lower(it) }
        }

        modules.forEach { module ->
            namer.merge(module.files, additionalPackages)
        }

        val code = EnumMap<TranslationMode, CompilationOutputs>(TranslationMode::class.java)

        if (fullJs) code[TranslationMode.fromFlags(false, multiModule)] = generateWrappedModuleBody(modules, exportedModule, namer)

        if (dceJs) {
            eliminateDeadDeclarations(modules, backendContext, removeUnusedAssociatedObjects)
            // Use a fresh namer for DCE so that we could compare the result with DCE-driven
            // TODO: is this mode relevant for scripting? If yes, refactor so that the external name tables are used here when needed.
            val namer = NameTables(emptyList(), context = backendContext)
            namer.merge(modules.flatMap { it.files }, additionalPackages)
            code[TranslationMode.fromFlags(true, multiModule)] = generateWrappedModuleBody(modules, exportedModule, namer)
        }

        return CompilerResult(code, dts)
    }

    private fun generateWrappedModuleBody(modules: Iterable<IrModuleFragment>, exportedModule: ExportedModule, namer: NameTables): CompilationOutputs {
        if (multiModule) {

            val refInfo = buildCrossModuleReferenceInfo(modules)

            val rM = modules.reversed()

            val main = rM.first()
            val others = rM.drop(1)

            val mainModule = generateWrappedModuleBody2(
                listOf(main),
                others,
                exportedModule,
                namer,
                refInfo,
                generateMainCall = true,
            )

            val dependencies = others.mapIndexed { index, module ->
                val moduleName = module.externalModuleName()

                val exportedDeclarations = ExportModelGenerator(backendContext, generateNamespacesForPackages = true).let { module.files.flatMap { file -> it.generateExport(file) } }

                moduleName to generateWrappedModuleBody2(
                    listOf(module),
                    others.drop(index + 1),
                    ExportedModule(moduleName, exportedModule.moduleKind, exportedDeclarations),
                    namer,
                    refInfo,
                    generateMainCall = false,
                )
            }.reversed()

            return CompilationOutputs(mainModule.jsCode, mainModule.jsProgram, mainModule.sourceMap, dependencies)
        } else {
            return generateWrappedModuleBody2(
                modules,
                emptyList(),
                exportedModule,
                namer,
                EmptyCrossModuleReferenceInfo,
                generateMainCall = true,
            )
        }
    }

    private fun generateWrappedModuleBody2(
        modules: Iterable<IrModuleFragment>,
        dependencies: Iterable<IrModuleFragment>,
        exportedModule: ExportedModule,
        namer: NameTables,
        refInfo: CrossModuleReferenceInfo,
        generateMainCall: Boolean = true,
    ): CompilationOutputs {

        val nameGenerator = refInfo.withReferenceTracking(
            IrNamerImpl(newNameTables = namer, backendContext),
            modules
        )
        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator,
            globalNameScope = namer.globalNames
        )

        val (importStatements, importedJsModules) =
            generateImportStatements(
                getNameForExternalDeclaration = { staticContext.getNameForStaticDeclaration(it) },
                declareFreshGlobal = { JsName(sanitizeName(it), false) } // TODO: Declare fresh name
            )

        val moduleBody = generateModuleBody(modules, staticContext)
        val internalModuleName = JsName("_", false)
        val globalNames = NameTable<String>(namer.globalNames)
        val exportStatements = ExportModelToJsStatements(nameGenerator) { globalNames.declareFreshName(it, it) }
            .generateModuleExport(exportedModule, internalModuleName)

        val (crossModuleImports, importedKotlinModules) = generateCrossModuleImports(nameGenerator, modules, dependencies, { JsName(sanitizeName(it), false) })
        val crossModuleExports = generateCrossModuleExports(modules, refInfo, internalModuleName)

        val program = JsProgram()

        if (generateScriptModule) {
            with(program.globalBlock) {
                statements.addWithComment("block: imports", importStatements + crossModuleImports)
                statements += moduleBody
                statements.addWithComment("block: exports", exportStatements + crossModuleExports)
            }
        } else {
            val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function").apply {
                parameters += JsParameter(internalModuleName)
                parameters += (importedJsModules + importedKotlinModules).map { JsParameter(it.internalName) }
                with(body) {
                    statements += JsStringLiteral("use strict").makeStmt()
                    statements.addWithComment("block: imports", importStatements + crossModuleImports)
                    statements += moduleBody
                    statements.addWithComment("block: exports", exportStatements + crossModuleExports)
                    if (generateMainCall) {
                        statements += generateCallToMain(modules, staticContext)
                    }
                    statements += JsReturn(internalModuleName.makeRef())
                }
            }

            program.globalBlock.statements += ModuleWrapperTranslation.wrap(
                exportedModule.name,
                rootFunction,
                importedJsModules + importedKotlinModules,
                program,
                kind = exportedModule.moduleKind
            )
        }

        val jsCode = TextOutputImpl()

        val configuration = backendContext.configuration
        val sourceMapPrefix = configuration.get(JSConfigurationKeys.SOURCE_MAP_PREFIX, "")
        val sourceMapsEnabled = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)

        val sourceMapBuilder = SourceMap3Builder(null, jsCode, sourceMapPrefix)
        val sourceMapBuilderConsumer =
            if (sourceMapsEnabled) {
                val sourceRoots = configuration.get(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, emptyList<String>()).map(::File)
                val generateRelativePathsInSourceMap = sourceMapPrefix.isEmpty() && sourceRoots.isEmpty()
                val outputDir = if (generateRelativePathsInSourceMap) configuration.get(JSConfigurationKeys.OUTPUT_DIR) else null

                val pathResolver = SourceFilePathResolver(sourceRoots, outputDir)

                val sourceMapContentEmbedding =
                    configuration.get(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, SourceMapSourceEmbedding.INLINING)

                SourceMapBuilderConsumer(
                    File("."),
                    sourceMapBuilder,
                    pathResolver,
                    sourceMapContentEmbedding == SourceMapSourceEmbedding.ALWAYS,
                    sourceMapContentEmbedding != SourceMapSourceEmbedding.NEVER
                )
            } else {
                null
            }

        program.accept(JsToStringGenerationVisitor(jsCode, sourceMapBuilderConsumer ?: NoOpSourceLocationConsumer))

        return CompilationOutputs(
            jsCode.toString(),
            program,
            if(sourceMapsEnabled) sourceMapBuilder.build() else null
        )
    }

    private fun IrModuleFragment.externalModuleName(): String {
        return moduleToName[this] ?: sanitizeName(safeName)
    }

    private fun generateCrossModuleImports(
        namerWithImports: IrNamerWithImports,
        currentModules: Iterable<IrModuleFragment>,
        allowedDependencies: Iterable<IrModuleFragment>,
        declareFreshGlobal: (String) -> JsName
    ): Pair<MutableList<JsStatement>, List<JsImportedModule>> {
        val imports = mutableListOf<JsStatement>()
        val modules = mutableListOf<JsImportedModule>()

        namerWithImports.imports().forEach { (module, names) ->
            check(module in allowedDependencies) {
                val deps = if (names.size > 10) "[${names.take(10).joinToString()}, ...]" else "$names"
                "Module ${currentModules.map { it.name.asString() }} depend on module ${module.name.asString()} via $deps"
            }

            val moduleName = declareFreshGlobal(module.safeName)
            modules += JsImportedModule(module.externalModuleName(), moduleName, null, relativeRequirePath)

            names.forEach {
                imports += JsVars(JsVars.JsVar(JsName(it, false), JsNameRef(it, JsNameRef("\$crossModule\$", moduleName.makeRef()))))
            }
        }

        return imports to modules
    }

    private fun generateCrossModuleExports(
        modules: Iterable<IrModuleFragment>,
        refInfo: CrossModuleReferenceInfo,
        internalModuleName: JsName
    ): List<JsStatement> {
        return modules.flatMap {
            refInfo.exports(it).map {
                jsAssignment(
                    JsNameRef(it, JsNameRef("\$crossModule\$", internalModuleName.makeRef())),
                    JsNameRef(it)
                ).makeStmt()
            }
        }.let {
            if (!it.isEmpty()) {
                val createExportBlock = jsAssignment(
                    JsNameRef("\$crossModule\$", internalModuleName.makeRef()),
                    JsAstUtils.or(JsNameRef("\$crossModule\$", internalModuleName.makeRef()), JsObjectLiteral())
                ).makeStmt()
                return listOf(createExportBlock) + it
            } else it
        }
    }

    private fun generateModuleBody(modules: Iterable<IrModuleFragment>, staticContext: JsStaticContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>()

        val preDeclarationBlock = JsGlobalBlock()

        val postDeclarationBlock = JsGlobalBlock()

        statements.addWithComment("block: pre-declaration", preDeclarationBlock)

        val generateFilePaths = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_COMMENTS_WITH_FILE_PATH)
        val pathPrefixMap = backendContext.configuration.getMap(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP)

        modules.forEach { module ->
            module.files.forEach {
                val fileStatements = it.accept(IrFileToJsTransformer(), staticContext).statements
                if (fileStatements.isNotEmpty()) {
                    var startComment = ""

                    if (generateRegionComments) {
                        startComment = "region "
                    }

                    if (generateRegionComments || generateFilePaths) {
                        val originalPath = it.path
                        val path = pathPrefixMap.entries
                            .find { (k, _) -> originalPath.startsWith(k) }
                            ?.let { (k, v) -> v + originalPath.substring(k.length) }
                            ?: originalPath

                        startComment += "file: $path"
                    }

                    if (startComment.isNotEmpty()) {
                        statements.add(JsSingleLineComment(startComment))
                    }

                    statements.addAll(fileStatements)
                    statements.endRegion()
                }
            }
        }

        // sort member forwarding code
        processClassModels(staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        statements.addWithComment("block: post-declaration", postDeclarationBlock.statements)
        statements.addWithComment("block: init", staticContext.initializerBlock.statements)

        modules.forEach { module ->
            val tests = module.files
                .groupBy({ it.fqName.asString()}) { backendContext.testFunsPerFile[it] }
                .mapNotNull{ (fqn, testFuns) -> testFuns.filterNotNull().let { if (it.isEmpty()) null else fqn to it } }
                .associate { it }

            if (tests.isNotEmpty()) {
                val testFunBody = JsBlock()
                val testFun = JsFunction(emptyScope, testFunBody, "root test fun")
                val suiteFunRef = staticContext.getNameForStaticFunction(backendContext.suiteFun!!.owner).makeRef()

                for ((pkg, testFuns) in tests) {
                    val pkgTestFun = JsFunction(emptyScope, JsBlock(), "test fun for $pkg")
                    pkgTestFun.body.statements += testFuns.map {
                        JsInvocation(staticContext.getNameForStaticFunction(it).makeRef()).makeStmt()
                    }
                    testFun.body.statements +=
                        JsInvocation(suiteFunRef, JsStringLiteral(pkg), JsBooleanLiteral(false), pkgTestFun).makeStmt()
                }

                statements.startRegion("block: tests")
                statements += JsInvocation(testFun).makeStmt()
                statements.endRegion()
            }
        }

        return statements
    }

    private fun generateMainArguments(
        generateArgv: Boolean,
        generateContinuation: Boolean,
        staticContext: JsStaticContext,
    ): List<JsExpression> {
        val mainArguments = this.mainArguments!!
        val mainArgumentsArray =
            if (generateArgv) JsArrayLiteral(mainArguments.map { JsStringLiteral(it) }) else null

        val continuation = if (generateContinuation) {
            backendContext.coroutineEmptyContinuation.owner
                .let { it.getter!! }
                .let { staticContext.getNameForStaticFunction(it) }
                .let { JsInvocation(it.makeRef()) }
        } else null

        return listOfNotNull(mainArgumentsArray, continuation)
    }

    private fun generateCallToMain(modules: Iterable<IrModuleFragment>, staticContext: JsStaticContext): List<JsStatement> {
        // TODO: Generate calls to main as IR->IR lowering
        if (mainArguments == null) return emptyList() // in case `NO_MAIN` and `main(..)` exists
        val mainFunction = JsMainFunctionDetector(backendContext).getMainFunctionOrNull(modules.last())
        return mainFunction?.let {
            val jsName = staticContext.getNameForStaticFunction(it)
            val generateArgv = it.valueParameters.firstOrNull()?.isStringArrayParameter() ?: false
            val generateContinuation = it.isLoweredSuspendFunction(backendContext)
            listOf(JsInvocation(jsName.makeRef(), generateMainArguments(generateArgv, generateContinuation, staticContext)).makeStmt())
        } ?: emptyList()
    }

    private fun generateImportStatements(
        getNameForExternalDeclaration: (IrDeclarationWithName) -> JsName,
        declareFreshGlobal: (String) -> JsName
    ): Pair<MutableList<JsStatement>, List<JsImportedModule>> {
        val declarationLevelJsModules =
            backendContext.declarationLevelJsModules.map { externalDeclaration ->
                val jsModule = externalDeclaration.getJsModule()!!
                val name = getNameForExternalDeclaration(externalDeclaration)
                JsImportedModule(jsModule, name, name.makeRef())
            }

        val packageLevelJsModules = mutableListOf<JsImportedModule>()
        val importStatements = mutableListOf<JsStatement>()

        for (file in backendContext.packageLevelJsModules) {
            val jsModule = file.getJsModule()
            val jsQualifier = file.getJsQualifier()

            assert(jsModule != null || jsQualifier != null)

            val qualifiedReference: JsExpression

            if (jsModule != null) {
                val internalName = declareFreshGlobal("\$module\$$jsModule")
                packageLevelJsModules += JsImportedModule(jsModule, internalName, null)

                qualifiedReference =
                    if (jsQualifier == null)
                        internalName.makeRef()
                    else
                        JsNameRef(jsQualifier, internalName.makeRef())
            } else {
                qualifiedReference = JsNameRef(jsQualifier!!)
            }

            file.declarations
                .asSequence()
                .filterIsInstance<IrDeclarationWithName>()
                .filter { !(it is IrClass && it.isInterface && it.isEffectivelyExternal()) }
                .forEach { declaration ->
                    val declName = getNameForExternalDeclaration(declaration)
                    importStatements.add(
                        JsVars(JsVars.JsVar(declName, jsElementAccess(declaration.getJsNameOrKotlinName().identifier, qualifiedReference)))
                    )
                }
        }

        val importedJsModules = (declarationLevelJsModules + packageLevelJsModules).distinctBy { it.key }
        return Pair(importStatements, importedJsModules)
    }


    private fun MutableList<JsStatement>.startRegion(description: String = "") {
        if (generateRegionComments) {
            this += JsSingleLineComment("region $description")
        }
    }

    private fun MutableList<JsStatement>.endRegion() {
        if (generateRegionComments) {
            this += JsSingleLineComment("endregion")
        }
    }

    private fun MutableList<JsStatement>.addWithComment(regionDescription: String = "", block: JsBlock) {
        startRegion(regionDescription)
        this += block
        endRegion()
    }

    private fun MutableList<JsStatement>.addWithComment(regionDescription: String = "", statements: List<JsStatement>) {
        if (statements.isEmpty()) return

        startRegion(regionDescription)
        this += statements
        endRegion()
    }
}

fun processClassModels(
    classModelMap: Map<IrClassSymbol, JsIrClassModel>,
    preDeclarationBlock: JsBlock,
    postDeclarationBlock: JsBlock
) {
    val declarationHandler = object : DFS.AbstractNodeHandler<IrClassSymbol, Unit>() {
        override fun result() {}
        override fun afterChildren(current: IrClassSymbol) {
            classModelMap[current]?.let {
                preDeclarationBlock.statements += it.preDeclarationBlock.statements
                postDeclarationBlock.statements += it.postDeclarationBlock.statements
            }
        }
    }

    DFS.dfs(
        classModelMap.keys,
        { klass -> classModelMap[klass]?.superClasses ?: emptyList() },
        declarationHandler
    )
}