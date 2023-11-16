/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.partitionIsInstance

class Merger(
    private val moduleName: String,
    private val moduleKind: ModuleKind,
    private val fragments: List<JsIrProgramFragment>,
    private val crossModuleReferences: CrossModuleReferences,
    private val generateRegionComments: Boolean,
    private val generateCallToMain: Boolean,
) {

    private val isEsModules = moduleKind == ModuleKind.ES
    private val importStatements = mutableMapOf<String, JsStatement>()
    private val importStatementsWithEffect = mutableListOf<JsStatement>()
    private val importedModulesMap = mutableMapOf<JsImportedModuleKey, JsImportedModule>()

    private val additionalExports = mutableListOf<JsStatement>()

    private fun linkJsNames() {
        val nameMap = mutableMapOf<String, JsName>()

        fragments.forEach { f ->
            f.buildRenames(nameMap).run {
                rename(f.declarations)
                rename(f.exports)

                f.imports.entries.forEach { (declaration, importStatement) ->
                    val importName = nameMap[declaration]

                    if (importName == null && !isEsModules) {
                        error("Missing name for declaration '${declaration}'")
                    }

                    importStatements.putIfAbsent(declaration, rename(importStatement.importStatementWithName(importName)))
                }

                val classModels = (mutableMapOf<JsName, JsIrIcClassModel>() + f.classes)
                    .also { f.classes.clear() }

                classModels.entries.forEach { (name, model) ->
                    f.classes[rename(name)] = JsIrIcClassModel(model.superClasses.map { rename(it) }).also {
                        it.preDeclarationBlock.statements += model.preDeclarationBlock.statements
                        it.postDeclarationBlock.statements += model.postDeclarationBlock.statements
                        rename(it.preDeclarationBlock)
                        rename(it.postDeclarationBlock)
                    }
                }

                rename(f.initializers)
                rename(f.eagerInitializers)
            }
        }

        for ((tag, crossModuleJsImport) in crossModuleReferences.jsImports) {
            val importName = nameMap[tag] ?: error("Missing name for declaration '$tag'")
            importStatements.putIfAbsent(tag, crossModuleJsImport.renameImportedSymbolInternalName(importName))
        }

        importStatementsWithEffect.addAll(crossModuleReferences.jsImportsWithEffect)

        if (crossModuleReferences.exports.isNotEmpty()) {
            val internalModuleName = ReservedJsNames.makeInternalModuleName()

            if (isEsModules) {
                val exportedElements = crossModuleReferences.exports.entries.map { (tag, hash) ->
                    val internalName = nameMap[tag] ?: error("Missing name for declaration '$tag'")
                    JsExport.Element(internalName.makeRef(), JsName(hash, false))
                }

                additionalExports += JsExport(JsExport.Subject.Elements(exportedElements))
            } else {
                val createExportBlock = jsAssignment(
                    ReservedJsNames.makeCrossModuleNameRef(internalModuleName),
                    JsAstUtils.or(ReservedJsNames.makeCrossModuleNameRef(internalModuleName), JsObjectLiteral())
                ).makeStmt()
                additionalExports += createExportBlock

                crossModuleReferences.exports.entries.forEach { (tag, hash) ->
                    val internalName = nameMap[tag] ?: error("Missing name for declaration '$tag'")
                    val crossModuleRef = ReservedJsNames.makeCrossModuleNameRef(ReservedJsNames.makeInternalModuleName())
                    additionalExports += jsAssignment(JsNameRef(hash, crossModuleRef), JsNameRef(internalName)).makeStmt()
                }
            }
        }
    }

    private fun JsIrProgramFragment.buildRenames(nameMap: MutableMap<String, JsName>): Map<JsName, JsName> {
        val result = mutableMapOf<JsName, JsName>()

        this.importedModules.forEach { module ->
            val existingModule = importedModulesMap.getOrPut(module.key) { module }
            if (existingModule !== module) {
                result[module.internalName] = existingModule.internalName
            }
        }

        this.nameBindings.entries.forEach { (tag, name) ->
            val existingName = nameMap.getOrPut(tag) { name }
            if (existingName !== name) {
                result[name] = existingName
            }
        }

        return result
    }

    private fun Map<JsName, JsName>.rename(name: JsName): JsName = getOrElse(name) { name }

    private fun <T : JsNode> Map<JsName, JsName>.rename(rootNode: T): T {
        rootNode.accept(object : RecursiveJsVisitor() {
            override fun visitElement(node: JsNode) {
                super.visitElement(node)
                if (node is HasName) {
                    node.name = node.name?.let { rename(it) }
                }
            }
        })
        return rootNode
    }

    private fun assertSingleDefinition() {
        val definitions = mutableSetOf<String>()
        fragments.forEach {
            it.definitions.forEach {
                if (!definitions.add(it)) {
                    error("Clashing definitions with tag '$it'")
                }
            }
        }
    }

    private fun declareAndCallJsExporter(): List<JsStatement> {
        if (isEsModules) {
            val allExportRelatedStatements = fragments.flatMap { it.exports.statements }
            val (allExportStatements, restStatements) = allExportRelatedStatements.partitionIsInstance<JsStatement, JsExport>()
            val (currentModuleExportStatements, restExportStatements) = allExportStatements.partition { it.fromModule == null }
            val exportedElements = currentModuleExportStatements.takeIf { it.isNotEmpty() }
                ?.asSequence()
                ?.flatMap { (it.subject as JsExport.Subject.Elements).elements }
                ?.distinctBy { it.alias?.ident ?: it.name.ident }
                ?.toList()

            val oneLargeExportStatement = exportedElements?.let { JsExport(JsExport.Subject.Elements(it)) }

            return restStatements + listOfNotNull(oneLargeExportStatement) + restExportStatements
        } else {
            val exportBody = JsBlock(fragments.flatMap { it.exports.statements })
            if (exportBody.isEmpty) {
                return emptyList()
            }

            val internalModuleName = ReservedJsNames.makeInternalModuleName()
            val exporterName = ReservedJsNames.makeJsExporterName()
            val jsExporterFunction = JsFunction(emptyScope, "js exporter function").apply {
                body = exportBody
                name = exporterName
                parameters.add(JsParameter(internalModuleName))
            }
            val jsExporterCall = JsInvocation(exporterName.makeRef(), internalModuleName.makeRef())
            val result = mutableListOf(jsExporterFunction.makeStmt(), jsExporterCall.makeStmt())
            if (!generateCallToMain) {
                val exportExporter = jsAssignment(JsNameRef(exporterName, internalModuleName.makeRef()), exporterName.makeRef())
                result += exportExporter.makeStmt()
            }
            return result
        }
    }

    private fun transitiveJsExport(): List<JsStatement> {
        return if (isEsModules) {
            crossModuleReferences.transitiveExportFrom.map {
                JsExport(JsExport.Subject.All, it.getRequireEsmName())
            }
        } else {
            val internalModuleName = ReservedJsNames.makeInternalModuleName()
            val exporterName = ReservedJsNames.makeJsExporterName()

            crossModuleReferences.transitiveExportFrom.map {
                JsInvocation(
                    JsNameRef(exporterName, it.internalName.makeRef()),
                    internalModuleName.makeRef()
                ).makeStmt()
            }
        }
    }

    fun merge(): JsProgram {
        assertSingleDefinition()

        linkJsNames()

        val moduleBody = mutableListOf<JsStatement>()

        val preDeclarationBlock = JsCompositeBlock()
        val postDeclarationBlock = JsCompositeBlock()
        val polyfillDeclarationBlock = JsCompositeBlock()

        moduleBody.addWithComment("block: pre-declaration", preDeclarationBlock)

        val classModels = mutableMapOf<JsName, JsIrIcClassModel>()
        val initializerBlock = JsCompositeBlock()

        fragments.forEach {
            moduleBody += it.declarations.statements
            classModels += it.classes
            initializerBlock.statements += it.initializers.statements + it.eagerInitializers.statements
            polyfillDeclarationBlock.statements += it.polyfills.statements
        }

        // sort member forwarding code
        processClassModels(classModels, preDeclarationBlock, postDeclarationBlock)

        moduleBody.addWithComment("block: post-declaration", postDeclarationBlock.statements)
        moduleBody.addWithComment("block: init", initializerBlock.statements)

        // Merge test function invocations
        JsTestFunctionTransformer.generateTestFunctionCall(fragments.asTestFunctionContainers())?.let {
            moduleBody.startRegion("block: tests")
            moduleBody += it.makeStmt()
            moduleBody.endRegion()
        }

        val fragmentWithMainFunction = JsMainFunctionDetector.pickMainFunctionFromCandidates(fragments) {
            JsMainFunctionDetector.MainFunctionCandidate(it.packageFqn, it.mainFunctionTag)
        }

        val exportStatements = declareAndCallJsExporter() + additionalExports + transitiveJsExport()

        val importedJsModules = this.importedModulesMap.values.toList() + this.crossModuleReferences.importedModules
        val importStatements = this.importStatements.values.toList() + this.importStatementsWithEffect.toList()

        val program = JsProgram()

        val internalModuleName = ReservedJsNames.makeInternalModuleName()
        val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function").apply {
            parameters += JsParameter(internalModuleName)
            parameters += (importedJsModules).map { JsParameter(it.internalName) }
            with(body) {
                if (!isEsModules) {
                    statements += JsStringLiteral("use strict").makeStmt()
                }
                statements.addWithComment("block: imports", importStatements)
                statements += moduleBody
                statements.addWithComment("block: exports", exportStatements)
                if (generateCallToMain && fragmentWithMainFunction != null) {
                    val mainFunctionTag = fragmentWithMainFunction.mainFunctionTag ?: error("Expect to have main function signature at this point")
                    val mainFunctionName = fragmentWithMainFunction.nameBindings[mainFunctionTag] ?: error("Expect to have name binding for tag $mainFunctionTag")
                    statements += JsInvocation(mainFunctionName.makeRef()).makeStmt()
                }
                this.statements += JsReturn(internalModuleName.makeRef())
            }
        }

        polyfillDeclarationBlock.statements
            .takeIf { it.isNotEmpty() }
            ?.let { program.globalBlock.statements.addWithComment("block: polyfills", it) }

        program.globalBlock.statements += ModuleWrapperTranslation.wrap(
            moduleName,
            rootFunction,
            importedJsModules,
            program,
            kind = moduleKind
        )

        return program
    }


    private fun processClassModels(
        classModelMap: Map<JsName, JsIrIcClassModel>,
        preDeclarationBlock: JsBlock,
        postDeclarationBlock: JsBlock
    ) {
        val declarationHandler = object : DFS.AbstractNodeHandler<JsName, Unit>() {
            override fun result() {}
            override fun afterChildren(current: JsName) {
                classModelMap[current]?.let {
                    preDeclarationBlock.statements += it.preDeclarationBlock.statements
                    postDeclarationBlock.statements += it.postDeclarationBlock.statements
                }
            }
        }

        DFS.dfs(
            classModelMap.keys,
            { classModelMap[it]?.superClasses ?: emptyList() },
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

    private fun JsStatement.importStatementWithName(name: JsName?): JsStatement {
        if (name == null) return this

        return when (this) {
            is JsVars -> JsVars(JsVars.JsVar(name, vars.single().initExpression))
            is JsImport -> JsImport(
                module,
                when (target) {
                    is JsImport.Target.Effect -> JsImport.Target.Effect
                    is JsImport.Target.All -> JsImport.Target.All(alias = name.makeRef())
                    is JsImport.Target.Default -> JsImport.Target.Default(name = name.makeRef())
                    is JsImport.Target.Elements -> JsImport.Target.Elements(
                        mutableListOf(
                            JsImport.Element(
                                elements.single().name,
                                name.makeRef()
                            )
                        )
                    )
                }
            )
            is JsCompositeBlock -> JsCompositeBlock(statements.dropLast(1) + statements.last().importStatementWithName(name))
            else -> error("Unexpected import statement ${this::class.qualifiedName}")
        }
    }
}

fun List<JsIrModule>.merge(): JsIrModule {
    assert(isNotEmpty()) { "Can't merge empty list of modules" }
    val firstModule = first()

    return if (size == 1) {
        firstModule
    } else {
        val fragments = mutableListOf<JsIrProgramFragment>()
        var reexportedInModuleWithName: String? = null

        for (module in this) {
            fragments.addAll(module.fragments)
            module.reexportedInModuleWithName?.let { reexportedInModuleWithName = it }
        }

        JsIrModule(firstModule.moduleName, firstModule.externalModuleName, fragments, reexportedInModuleWithName)
    }
}

fun List<JsIrModuleHeader>.merge(): JsIrModuleHeader {
    assert(isNotEmpty()) { "Can't merge empty list of module headers" }
    val firstModule = first()

    return if (size == 1) {
        firstModule
    } else {
        val definitions = mutableSetOf<String>()
        val nameBindings = mutableMapOf<String, String>()
        val optionalCrossModuleImports = mutableSetOf<String>()
        var reexportedInModuleWithName: String? = null
        var importedWithEffectInModuleWithName: String? = null

        for (header in this) {
            definitions.addAll(header.definitions)
            nameBindings.putAll(header.nameBindings)
            optionalCrossModuleImports.addAll(header.optionalCrossModuleImports)

            header.reexportedInModuleWithName?.let { reexportedInModuleWithName = it }
            header.importedWithEffectInModuleWithName?.let { importedWithEffectInModuleWithName = it }
        }

        JsIrModuleHeader(
            firstModule.moduleName,
            firstModule.externalModuleName,
            definitions,
            nameBindings,
            optionalCrossModuleImports,
            reexportedInModuleWithName,
            importedWithEffectInModuleWithName,
            null
        )
    }
}
