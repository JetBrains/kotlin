/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.JsCode
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
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.DFS

class IrModuleToJsTransformer(
    private val backendContext: JsIrBackendContext,
    private val mainArguments: List<String>?,
    private val generateScriptModule: Boolean = false,
    var namer: NameTables = NameTables(emptyList(), context = backendContext),
    private val fullJs: Boolean = true,
    private val dceJs: Boolean = false,
    private val multiModule: Boolean = false,
    private val relativeRequirePath: Boolean = false
) {
    private val generateRegionComments = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_REGION_COMMENTS)

    fun generateModule(modules: Iterable<IrModuleFragment>): CompilerResult {
        val additionalPackages = with(backendContext) {
            externalPackageFragment.values + listOf(
                bodilessBuiltInsPackageFragment,
                intrinsics.externalPackageFragment
            ) + packageLevelJsModules
        }

        val exportedModule = ExportModelGenerator(backendContext).generateExport(modules)
        val dts = exportedModule.toTypeScript()

        modules.forEach { module ->
            module.files.forEach { StaticMembersLowering(backendContext).lower(it) }
        }

        if (multiModule) {
            breakCrossModuleFieldAccess(backendContext, modules)
        }

        modules.forEach { module ->
            namer.merge(module.files, additionalPackages)
        }

        val jsCode = if (fullJs) generateWrappedModuleBody(modules, exportedModule, namer) else null

        val dceJsCode = if (dceJs) {
            eliminateDeadDeclarations(modules, backendContext)
            // Use a fresh namer for DCE so that we could compare the result with DCE-driven
            // TODO: is this mode relevant for scripting? If yes, refactor so that the external name tables are used here when needed.
            val namer = NameTables(emptyList(), context = backendContext)
            namer.merge(modules.flatMap { it.files }, additionalPackages)
            generateWrappedModuleBody(modules, exportedModule, namer)
        } else null

        return CompilerResult(jsCode, dceJsCode, dts)
    }

    private fun generateWrappedModuleBody(modules: Iterable<IrModuleFragment>, exportedModule: ExportedModule, namer: NameTables): JsCode {
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
                refInfo
            )

            val dependencies = others.mapIndexed { index, module ->
                val moduleName = sanitizeName(module.safeName)

                val exportedDeclarations = ExportModelGenerator(backendContext).let { module.files.flatMap { file -> it.generateExport(file) } }

                moduleName to generateWrappedModuleBody2(
                    listOf(module),
                    others.drop(index + 1),
                    ExportedModule(moduleName, exportedModule.moduleKind, exportedDeclarations),
                    namer,
                    refInfo
                )
            }.reversed()

            return JsCode(mainModule, dependencies)
        } else {
            return JsCode(
                generateWrappedModuleBody2(
                    modules,
                    emptyList(),
                    exportedModule,
                    namer,
                    EmptyCrossModuleReferenceInfo
                )
            )
        }
    }

    private fun generateWrappedModuleBody2(
        modules: Iterable<IrModuleFragment>,
        dependencies: Iterable<IrModuleFragment>,
        exportedModule: ExportedModule,
        namer: NameTables,
        refInfo: CrossModuleReferenceInfo
    ): String {

        val nameGenerator = refInfo.withReferenceTracking(
            IrNamerImpl(newNameTables = namer),
            modules
        )
        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator
        )
        val rootContext = JsGenerationContext(
            currentFunction = null,
            staticContext = staticContext
        )

        val (importStatements, importedJsModules) =
            generateImportStatements(
                getNameForExternalDeclaration = { rootContext.getNameForStaticDeclaration(it) },
                declareFreshGlobal = { JsName(sanitizeName(it)) } // TODO: Declare fresh name
            )

        val moduleBody = generateModuleBody(modules, rootContext)

        val internalModuleName = JsName("_")
        val globalNames = NameTable<String>(namer.globalNames)
        val exportStatements = ExportModelToJsStatements(internalModuleName, nameGenerator, { globalNames.declareFreshName(it, it)}).generateModuleExport(exportedModule)

        val callToMain = generateCallToMain(modules, rootContext)

        val (crossModuleImports, importedKotlinModules) = generateCrossModuleImports(nameGenerator, modules, dependencies, { JsName(sanitizeName(it)) })
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
                    statements.addWithComment("block: imports", importStatements + crossModuleImports)
                    statements += moduleBody
                    statements.addWithComment("block: exports", exportStatements + crossModuleExports)
                    statements += callToMain
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

        return program.toString()
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
            modules += JsImportedModule(moduleName.ident, moduleName, moduleName.makeRef(), relativeRequirePath)

            names.forEach {
                imports += JsVars(JsVars.JsVar(JsName(it), JsNameRef(it, JsNameRef("\$crossModule\$", moduleName.makeRef()))))
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

    private fun generateModuleBody(modules: Iterable<IrModuleFragment>, context: JsGenerationContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>().also {
            if (!generateScriptModule) it += JsStringLiteral("use strict").makeStmt()
        }

        val preDeclarationBlock = JsGlobalBlock()
        val postDeclarationBlock = JsGlobalBlock()

        statements.addWithComment("block: pre-declaration", preDeclarationBlock)

        val generateFilePaths = backendContext.configuration.getBoolean(JSConfigurationKeys.GENERATE_COMMENTS_WITH_FILE_PATH)
        val pathPrefixMap = backendContext.configuration.getMap(JSConfigurationKeys.FILE_PATHS_PREFIX_MAP)

        modules.forEach { module ->
            module.files.forEach {
                val fileStatements = it.accept(IrFileToJsTransformer(), context).statements
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
        processClassModels(context.staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        statements.addWithComment("block: post-declaration", postDeclarationBlock.statements)
        statements.addWithComment("block: init", context.staticContext.initializerBlock.statements)

        modules.forEach {
            backendContext.testRoots[it]?.let { testContainer ->
                statements.startRegion("block: tests")
                statements += JsInvocation(context.getNameForStaticFunction(testContainer).makeRef()).makeStmt()
                statements.endRegion()
            }
        }

        return statements
    }

    private fun generateMainArguments(mainFunction: IrSimpleFunction, rootContext: JsGenerationContext): List<JsExpression> {
        val mainArguments = this.mainArguments!!
        val mainArgumentsArray =
            if (mainFunction.valueParameters.isNotEmpty()) JsArrayLiteral(mainArguments.map { JsStringLiteral(it) }) else null

        val continuation = if (mainFunction.isSuspend) {
            backendContext.coroutineEmptyContinuation.owner
                .let { it.getter!! }
                .let { rootContext.getNameForStaticFunction(it) }
                .let { JsInvocation(it.makeRef()) }
        } else null

        return listOfNotNull(mainArgumentsArray, continuation)
    }

    private fun generateCallToMain(modules: Iterable<IrModuleFragment>, rootContext: JsGenerationContext): List<JsStatement> {
        if (mainArguments == null) return emptyList() // in case `NO_MAIN` and `main(..)` exists
        val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(modules.last())
        return mainFunction?.let {
            val jsName = rootContext.getNameForStaticFunction(it)
            listOf(JsInvocation(jsName.makeRef(), generateMainArguments(it, rootContext)).makeStmt())
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

            val qualifiedReference: JsNameRef

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
                        JsVars(JsVars.JsVar(declName, JsNameRef(declName, qualifiedReference)))
                    )
                }
        }

        val importedJsModules = (declarationLevelJsModules + packageLevelJsModules).distinctBy { it.key }
        return Pair(importStatements, importedJsModules)
    }

    private fun processClassModels(
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
