/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

class IrModuleToJsTransformer(
    private val backendContext: JsIrBackendContext,
    private val mainFunction: IrSimpleFunction?,
    private val mainArguments: List<String>?
) : BaseIrElementToJsNodeTransformer<JsNode, Nothing?> {

    val moduleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!

    private fun generateModuleBody(module: IrModuleFragment, context: JsGenerationContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>(
            JsStringLiteral("use strict").makeStmt()
        )

        // TODO: fix it up with new name generator
        val anyName = context.getNameForClass(backendContext.irBuiltIns.anyClass.owner)
        val throwableName = context.getNameForClass(backendContext.irBuiltIns.throwableClass.owner)
        val stringName = context.getNameForClass(backendContext.irBuiltIns.stringClass.owner)

        statements += JsVars(JsVars.JsVar(anyName, Namer.JS_OBJECT))
        statements += JsVars(JsVars.JsVar(throwableName, Namer.JS_ERROR))
        statements += JsVars(JsVars.JsVar(stringName, JsNameRef("String")))

        val preDeclarationBlock = JsBlock()
        val postDeclarationBlock = JsBlock()

        statements += preDeclarationBlock

        module.files.forEach {
            statements.add(JsDocComment(mapOf("file" to it.path)).makeStmt())
            statements.add(it.accept(IrFileToJsTransformer(), context))
        }

        // sort member forwarding code
        processClassModels(context.staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        statements += postDeclarationBlock
        statements += context.staticContext.initializerBlock

        if (backendContext.hasTests) {
            statements += JsInvocation(context.getNameForStaticFunction(backendContext.testContainer).makeRef()).makeStmt()
        }

        return statements
    }

    private fun generateExportStatements(
        module: IrModuleFragment,
        context: JsGenerationContext,
        internalModuleName: JsName
    ): List<JsStatement> {
        val exports = mutableListOf<JsExpressionStatement>()

        for (file in module.files) {
            for (declaration in file.declarations) {
                exports.addIfNotNull(
                    generateExportStatement(declaration, context, internalModuleName)
                )
            }
        }

        return exports
    }

    private fun generateExportStatement(
        declaration: IrDeclaration,
        context: JsGenerationContext,
        internalModuleName: JsName
    ): JsExpressionStatement? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration.visibility != Visibilities.PUBLIC) {
            return null
        }

        if (declaration.isEffectivelyExternal())
            return null

        if (declaration is IrClass && declaration.isCompanion)
            return null

        val name: JsName = when (declaration) {
            is IrSimpleFunction -> context.getNameForStaticFunction(declaration)
            is IrClass -> context.getNameForClass(declaration)
            // TODO: Fields must be exported as properties
            is IrField -> context.getNameForField(declaration)
            else -> return null
        }

        val exportName = sanitizeName(declaration.getJsNameOrKotlinName().asString())

        val expression =
            if (declaration is IrClass && declaration.isObject) {
                // TODO: Use export names for properties
                defineProperty(internalModuleName.makeRef(), name.ident, getter = JsNameRef("${name.ident}_getInstance"))
            } else {
                jsAssignment(JsNameRef(exportName, internalModuleName.makeRef()), name.makeRef())
            }

        return JsExpressionStatement(expression)
    }



    private fun generateModule(module: IrModuleFragment): JsProgram {
        val additionalPackages = with(backendContext) {
            externalPackageFragment.values + listOf(
                bodilessBuiltInsPackageFragment,
                intrinsics.externalPackageFragment
            ) + packageLevelJsModules
        }

        val namer = NameTables(module.files + additionalPackages)

        val program = JsProgram()

        val nameGenerator = IrNamerImpl(
            memberNameGenerator = LegacyMemberNameGenerator(program.rootScope),
            newNameTables = namer,
            rootScope = program.rootScope
        )
        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator,
            rootScope = program.rootScope
        )
        val rootContext = JsGenerationContext(
            parent = null,
            currentBlock = program.globalBlock,
            currentFunction = null,
            currentScope = program.rootScope,
            staticContext = staticContext
        )

        val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function")
        val internalModuleName = rootFunction.scope.declareName("_")

        val (importStatements, importedJsModules) =
            generateImportStatements(
                getNameForExternalDeclaration = { rootContext.getNameForStaticDeclaration(it) },
                declareFreshGlobal = { rootFunction.scope.declareFreshName(sanitizeName(it)) }
            )

        val moduleBody = generateModuleBody(module, rootContext)
        val exportStatements = generateExportStatements(module, rootContext, internalModuleName)

        with(rootFunction) {
            parameters += JsParameter(internalModuleName)
            parameters += importedJsModules.map { JsParameter(it.internalName) }
            with(body) {
                statements += importStatements
                statements += moduleBody
                statements += exportStatements
                statements += generateCallToMain(rootContext)
                statements += JsReturn(internalModuleName.makeRef())
            }
        }

        program.globalBlock.statements += ModuleWrapperTranslation.wrap(
            moduleName,
            rootFunction,
            importedJsModules,
            program,
            kind = moduleKind
        )

        return program
    }

    private fun generateMainArguments(mainFunction: IrSimpleFunction, rootContext: JsGenerationContext): List<JsExpression> {
        val mainArguments = this.mainArguments!!
        val mainArgumentsArray =
            if (mainFunction.valueParameters.isNotEmpty()) JsArrayLiteral(mainArguments.map { JsStringLiteral(it) }) else null

        val continuation = if (mainFunction.isSuspend) {
            val emptyContinuationField = backendContext.coroutineEmptyContinuation.owner
            rootContext.getNameForField(emptyContinuationField).makeRef()
        } else null

        return listOfNotNull(mainArgumentsArray, continuation)
    }

    private fun generateCallToMain(rootContext: JsGenerationContext): List<JsStatement> {
        if (mainArguments == null) return emptyList() // in case `NO_MAIN` and `main(..)` exists
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