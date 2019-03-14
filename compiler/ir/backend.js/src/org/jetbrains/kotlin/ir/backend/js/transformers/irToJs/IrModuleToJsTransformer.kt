/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.DFS

class IrModuleToJsTransformer(private val backendContext: JsIrBackendContext) : BaseIrElementToJsNodeTransformer<JsNode, Nothing?> {
    val moduleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!

    private fun generateModuleBody(module: IrModuleFragment, context: JsGenerationContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>()

        // TODO: fix it up with new name generator
        val anyName = context.getNameForSymbol(backendContext.irBuiltIns.anyClass)
        val throwableName = context.getNameForSymbol(backendContext.irBuiltIns.throwableClass)

        statements += JsVars(JsVars.JsVar(anyName, Namer.JS_OBJECT))
        statements += JsVars(JsVars.JsVar(throwableName, Namer.JS_ERROR))

        val preDeclarationBlock = JsBlock()
        val postDeclarationBlock = JsBlock()

        statements += preDeclarationBlock

        module.files.forEach {
            statements.add(it.accept(IrFileToJsTransformer(), context))
        }

        // sort member forwarding code
        processClassModels(context.staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        statements += postDeclarationBlock
        statements += context.staticContext.initializerBlock

        return statements
    }

    private fun generateExportStatements(
        module: IrModuleFragment,
        context: JsGenerationContext,
        internalModuleName: JsName
    ): List<JsStatement> {
        return module.files
            .flatMap { it.declarations }
            .asSequence()
            .filterIsInstance<IrDeclarationWithVisibility>()
            .filter { it.visibility == Visibilities.PUBLIC }
            .filter { !it.isEffectivelyExternal() }
            .filterIsInstance<IrSymbolOwner>()
            .map { declaration ->
                val name = context.getNameForSymbol(declaration.symbol)

                JsExpressionStatement(
                    if (declaration is IrClass && declaration.isObject) {
                        defineProperty(internalModuleName.makeRef(), name.ident, getter = JsNameRef("${name.ident}_getInstance"))
                    } else {
                        jsAssignment(JsNameRef(name, internalModuleName.makeRef()), name.makeRef())
                    }
                )
            }
            .toList()
    }

    private fun generateModule(module: IrModuleFragment): JsProgram {
        val program = JsProgram()
        val rootContext = JsGenerationContext(JsRootScope(program), backendContext)

        val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function")
        val internalModuleName = rootFunction.scope.declareName("_")

        rootFunction.parameters += JsParameter(internalModuleName)

        val declarationLevelJsModules =
            backendContext.declarationLevelJsModules.map { externalDeclaration ->
                val jsModule = externalDeclaration.getJsModule()!!
                val name = rootContext.getNameForDeclaration(externalDeclaration)
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
                val internalName = rootFunction.scope.declareFreshName(sanitizeName("\$module\$$jsModule"))
                packageLevelJsModules += JsImportedModule(jsModule, internalName, null)

                qualifiedReference =
                    if (jsQualifier == null)
                        internalName.makeRef()
                    else
                        JsNameRef(jsQualifier, internalName.makeRef())
            } else {
                qualifiedReference = JsNameRef(jsQualifier!!)
            }

            file.declarations.forEach { declaration ->
                val declName = rootContext.getNameForDeclaration(declaration)
                importStatements.add(
                    JsExpressionStatement(
                        jsAssignment(
                            declName.makeRef(),
                            JsNameRef(declName, qualifiedReference)
                        )
                    )
                )
            }
        }

        for (externalClass in backendContext.externalNestedClasses) {

            // External companions are not "nested". Instead they use parent class name.
            if (externalClass.isCompanion)
                continue

            val declName = rootContext.getNameForDeclaration(externalClass)
            val parentName = rootContext.getNameForDeclaration(externalClass.parentAsClass)
            importStatements.add(
                JsExpressionStatement(
                    jsAssignment(
                        declName.makeRef(),
                        JsNameRef(externalClass.getJsNameOrKotlinName().identifier, parentName.makeRef())
                    )
                )
            )
        }

        val importedJsModules = declarationLevelJsModules + packageLevelJsModules
        rootFunction.parameters += importedJsModules.map { JsParameter(it.internalName) }

        rootFunction.body.statements += importStatements

        val moduleBody = generateModuleBody(module, rootContext)

        val exportStatements = generateExportStatements(module, rootContext, internalModuleName)

        rootFunction.body.statements += moduleBody

        rootFunction.body.statements += exportStatements

        rootFunction.body.statements += JsReturn(internalModuleName.makeRef())

        program.globalBlock.statements += ModuleWrapperTranslation.wrap(
            moduleName,
            rootFunction,
            importedJsModules,
            program,
            kind = moduleKind
        )

        return program
    }

    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): JsNode =
        generateModule(declaration)

    private fun processClassModels(
        classModelMap: Map<JsName, JsClassModel>,
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

        DFS.dfs(classModelMap.keys, {
            val neighbors = mutableListOf<JsName>()
            classModelMap[it]?.run {
                if (superName != null) neighbors += superName!!
                neighbors += interfaces
            }
            neighbors
        }, declarationHandler)
    }
}