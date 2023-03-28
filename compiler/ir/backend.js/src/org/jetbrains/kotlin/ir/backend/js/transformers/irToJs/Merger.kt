/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

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
    private val importedModulesMap = mutableMapOf<JsImportedModuleKey, JsImportedModule>()

    private val additionalExports = mutableListOf<JsStatement>()

    private fun linkJsNames() {
        val nameMap = mutableMapOf<String, JsName>()
        val namespaceNameMap = mutableMapOf<String, JsName>()

        fragments.forEach { f ->
            f.buildRenames(nameMap, namespaceNameMap).run {
                rename(f.declarations)
                rename(f.exports)

                f.imports.entries.forEach { (declaration, importExpression) ->
                    val importName = nameMap[declaration] ?: error("Missing name for declaration '${declaration}'")
                    importStatements.putIfAbsent(declaration, JsVars(JsVars.JsVar(importName, rename(importExpression))))
                }

                val classModels = (mutableMapOf<JsName, JsIrIcClassModel>() + f.classes)
                    .also { f.classes.clear() }

                classModels.entries.forEach { (name, model) ->
                    val superClasses = model.superClasses.map { rename(it) }
                    val classNsVar = model.metadataInitialization.classNamespace?.let {
                        ClassNamespace(it.namespace, rename(it.variableName))
                    }
                    val metadataInit = rename(model.metadataInitialization.initializationStatement)
                    val afterDeclarations = model.metadataInitialization.afterDeclarations
                    val metadataInitialization = ClassMetadataInitialization(metadataInit, classNsVar, afterDeclarations)
                    f.classes[rename(name)] = JsIrIcClassModel(superClasses, metadataInitialization).also {
                        it.preDeclarationBlock.statements += model.preDeclarationBlock.statements
                        it.postDeclarationBlock.statements += model.postDeclarationBlock.statements
                        rename(it.preDeclarationBlock)
                        rename(it.postDeclarationBlock)
                    }
                }

                rename(f.initializers)
                f.mainFunction?.let { rename(it) }
                f.testFunInvocation?.let { rename(it) }
                f.suiteFn?.let { f.suiteFn = rename(it) }
            }
        }

        for ((tag, crossModuleJsImport) in crossModuleReferences.jsImports) {
            val importName = nameMap[tag] ?: error("Missing name for declaration '$tag'")

            importStatements.putIfAbsent(tag, crossModuleJsImport.renameImportedSymbolInternalName(importName))
        }

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

    private fun JsIrProgramFragment.buildRenames(
        nameMap: MutableMap<String, JsName>,
        namespaceNameMap: MutableMap<String, JsName>
    ): Map<JsName, JsName> {
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

        this.classes.values.forEach {
            val classNamespace = it.metadataInitialization.classNamespace
            if (classNamespace != null) {
                val usedNamespaceName = namespaceNameMap[classNamespace.namespace]
                if (usedNamespaceName == null) {
                    namespaceNameMap[classNamespace.namespace] = classNamespace.variableName
                    result[classNamespace.variableName] = classNamespace.variableName
                } else {
                    result[classNamespace.variableName] = usedNamespaceName
                }
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
                ?.map { if (it.name.ident == it.alias?.ident) JsExport.Element(it.name, null) else it }
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
            crossModuleReferences.transitiveJsExportFrom.map {
                JsExport(JsExport.Subject.All, it.getRequireEsmName())
            }
        } else {
            val internalModuleName = ReservedJsNames.makeInternalModuleName()
            val exporterName = ReservedJsNames.makeJsExporterName()

            crossModuleReferences.transitiveJsExportFrom.map {
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
        val polyfillDeclarationBlock = JsCompositeBlock()

        val rawClassModels = buildMap { fragments.forEach { putAll(it.classes) } }

        // sort member forwarding code
        val classModels = processClassModels(rawClassModels)

        moduleBody.addWithComment("block: pre-declaration", classModels.preDeclarationBlock)
        moduleBody.addMetadataInitializationBlock("block: metadata pre-initialization", classModels.preMetadataInitialization)

        val initializerBlock = JsCompositeBlock()

        fragments.forEach {
            moduleBody += it.declarations.statements
            initializerBlock.statements += it.initializers.statements
            polyfillDeclarationBlock.statements += it.polyfills.statements
        }

        moduleBody.addWithComment("block: post-declaration", classModels.postDeclarationBlock)
        moduleBody.addMetadataInitializationBlock("block: metadata post-initialization", classModels.postMetadataInitialization)

        moduleBody.addWithComment("block: init", initializerBlock.statements)

        // Merge test function invocations
        if (fragments.any { it.testFunInvocation != null }) {
            val testFunBody = JsBlock()
            val testFun = JsFunction(emptyScope, testFunBody, "root test fun")
            val suiteFunRef = fragments.firstNotNullOf { it.suiteFn }.makeRef()

            val tests = fragments.filter { it.testFunInvocation != null }
                .groupBy({ it.packageFqn }) { it.testFunInvocation } // String -> [IrSimpleFunction]

            for ((pkg, testCalls) in tests) {
                val pkgTestFun = JsFunction(emptyScope, JsBlock(), "test fun for $pkg")
                pkgTestFun.body.statements += testCalls
                testFun.body.statements += JsInvocation(suiteFunRef, JsStringLiteral(pkg), JsBooleanLiteral(false), pkgTestFun).makeStmt()
            }

            moduleBody.startRegion("block: tests")
            moduleBody += JsInvocation(testFun).makeStmt()
            moduleBody.endRegion()
        }

        val callToMain = fragments.sortedBy { it.packageFqn }.firstNotNullOfOrNull { it.mainFunction }

        val exportStatements = declareAndCallJsExporter() + additionalExports + transitiveJsExport()

        val importedJsModules = this.importedModulesMap.values.toList() + this.crossModuleReferences.importedModules
        val importStatements = this.importStatements.values.toList()

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
                if (generateCallToMain) {
                    callToMain?.let { this.statements += it }
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

    private class MetadataInitialization {
        val classNamespaces = mutableListOf<ClassNamespace>()
        val initializationStatement = mutableListOf<JsStatement>()
    }

    private class ProcessedClassModels {
        val preDeclarationBlock = mutableListOf<JsStatement>()
        val postDeclarationBlock = mutableListOf<JsStatement>()

        val preMetadataInitialization = MetadataInitialization()
        val postMetadataInitialization = MetadataInitialization()
    }

    private fun processClassModels(classModelMap: Map<JsName, JsIrIcClassModel>): ProcessedClassModels {
        val declarationHandler = object : DFS.AbstractNodeHandler<JsName, Unit>() {
            val result = ProcessedClassModels()

            override fun result() {}
            override fun afterChildren(current: JsName) {
                classModelMap[current]?.let {
                    result.preDeclarationBlock += it.preDeclarationBlock.statements
                    result.postDeclarationBlock += it.postDeclarationBlock.statements

                    val metadataInitLocation = if (it.metadataInitialization.afterDeclarations) {
                        result.postMetadataInitialization
                    } else {
                        result.preMetadataInitialization
                    }

                    metadataInitLocation.initializationStatement += it.metadataInitialization.initializationStatement
                    if (it.metadataInitialization.classNamespace != null) {
                        metadataInitLocation.classNamespaces += it.metadataInitialization.classNamespace
                    }
                }
            }
        }

        DFS.dfs(
            classModelMap.keys,
            { classModelMap[it]?.superClasses ?: emptyList() },
            declarationHandler
        )

        return declarationHandler.result
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

    private fun MutableList<JsStatement>.addWithComment(regionDescription: String = "", statements: List<JsStatement>) {
        if (statements.isEmpty()) return

        startRegion(regionDescription)
        this += statements
        endRegion()
    }

    private fun MutableList<JsStatement>.addMetadataInitializationBlock(regionDescription: String = "", metadata: MetadataInitialization) {
        if (metadata.initializationStatement.isEmpty()) return

        startRegion(regionDescription)
        val varsInitialization = buildNamespaceVarsInitialization(metadata.classNamespaces)
        val body = JsBlock()
        if (!varsInitialization.isEmpty) {
            body.statements += varsInitialization
        }
        body.statements += metadata.initializationStatement
        val function = JsFunction(emptyScope, body, regionDescription)
        this += JsInvocation(function).makeStmt()
        endRegion()
    }
}
