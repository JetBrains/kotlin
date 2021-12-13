/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.emptyScope
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.DFS

class Merger(
    private val moduleName: String,
    private val moduleKind: ModuleKind,
    private val fragments: List<JsIrProgramFragment>,
    private val crossModuleReferences: CrossModuleReferences,
    private val generateScriptModule: Boolean,
    private val generateRegionComments: Boolean,
    private val generateCallToMain: Boolean,
) {

    private val importStatements = mutableMapOf<String, JsStatement>()
    private val importedModulesMap = mutableMapOf<JsImportedModuleKey, JsImportedModule>()

    private val additionalExports = mutableListOf<JsStatement>()

    private fun linkJsNames() {
        val nameMap = mutableMapOf<String, JsName>()

        fragments.forEach { f ->
            f.buildRenames(nameMap).run {
                rename(f.declarations)
                rename(f.exports)

                f.imports.entries.forEach { (declaration, importExpression) ->
                    val importName = nameMap[declaration] ?: error("Missing name for declaration '${declaration}'")
                    importStatements.putIfAbsent(declaration, JsVars(JsVars.JsVar(importName, rename(importExpression))))
                }

                val classModels = mutableMapOf<JsName, JsIrIcClassModel>() + f.classes
                f.classes.clear()
                classModels.entries.forEach { (name, model) ->
                    f.classes[rename(name)] = JsIrIcClassModel(model.superClasses.map { rename(it) }).also {
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

        for ((tag, jsVar) in crossModuleReferences.imports) {
            val importName = nameMap[tag] ?: error("Missing name for declaration '$tag'")

            importStatements.putIfAbsent(tag, JsVars(JsVars.JsVar(importName, jsVar.initExpression)))
        }

        if (crossModuleReferences.exports.isNotEmpty()) {
            val internalModuleName = JsName("_", false)

            val createExportBlock = jsAssignment(
                JsNameRef("\$crossModule\$", internalModuleName.makeRef()),
                JsAstUtils.or(JsNameRef("\$crossModule\$", internalModuleName.makeRef()), JsObjectLiteral())
            ).makeStmt()
            additionalExports += createExportBlock

            crossModuleReferences.exports.entries.forEach { (tag, name) ->
                val internalName = nameMap[tag] ?: error("Missing name for declaration '$tag'")
                additionalExports += jsAssignment(
                    JsNameRef(name, JsNameRef("\$crossModule\$", JsName("_", false).makeRef())),
                    JsNameRef(internalName)
                ).makeStmt()
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

    fun merge(): JsProgram {
        assertSingleDefinition()

        linkJsNames()

        val moduleBody = mutableListOf<JsStatement>()

        val polyfills = JsPolyfills(generateRegionComments)
            .apply { fragments.forEach { this += it.polyfills } }

        val preDeclarationBlock = JsGlobalBlock()
        val postDeclarationBlock = JsGlobalBlock()

        moduleBody.addWithComment("block: pre-declaration", preDeclarationBlock)

        val classModels = mutableMapOf<JsName, JsIrIcClassModel>()
        val initializerBlock = JsGlobalBlock()
        fragments.forEach {
            moduleBody += it.declarations.statements
            classModels += it.classes
            initializerBlock.statements += it.initializers.statements
        }

        // sort member forwarding code
        processClassModels(classModels, preDeclarationBlock, postDeclarationBlock)

        moduleBody.addWithComment("block: post-declaration", postDeclarationBlock.statements)
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

        val exportStatements = fragments.flatMap { it.exports.statements } + additionalExports

        val importedJsModules = this.importedModulesMap.values.toList() + this.crossModuleReferences.importedModules
        val importStatements = this.importStatements.values.toList()

        val internalModuleName = JsName("_", false)

        val program = JsProgram().apply {
            polyfills.addAllNeededPolyfillsTo(globalBlock.statements)
        }

        if (generateScriptModule) {
            with(program.globalBlock) {
                if (!generateScriptModule) {
                    statements += JsStringLiteral("use strict").makeStmt()
                }
                statements.addWithComment("block: imports", importStatements)
                statements += moduleBody
                statements.addWithComment("block: exports", exportStatements)
            }
        } else {
            val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function").apply {
                parameters += JsParameter(internalModuleName)
                parameters += (importedJsModules).map { JsParameter(it.internalName) }
                with(body) {
                    if (!generateScriptModule) {
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

            program.globalBlock.statements += ModuleWrapperTranslation.wrap(
                moduleName,
                rootFunction,
                importedJsModules,
                program,
                kind = moduleKind
            )
        }

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