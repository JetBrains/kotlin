/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.CompilerResult
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelGenerator
import org.jetbrains.kotlin.ir.backend.js.export.ExportModelToJsStatements
import org.jetbrains.kotlin.ir.backend.js.export.toTypeScript
import org.jetbrains.kotlin.ir.backend.js.lower.StaticMembersLowering
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.utils.DFS

class IrModuleToJsTransformer(
    private val backendContext: JsIrBackendContext,
    private val mainFunction: IrSimpleFunction?,
    private val mainArguments: List<String>?,
    private val generateScriptModule: Boolean = false,
    var namer: NameTables = NameTables(emptyList())
) {
    val moduleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!
    private val moduleKind = backendContext.configuration[JSConfigurationKeys.MODULE_KIND]!!

    private fun generateModuleBody(module: IrModuleFragment, context: JsGenerationContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>().also {
            if (!generateScriptModule) it += JsStringLiteral("use strict").makeStmt()
        }

        val preDeclarationBlock = JsBlock()
        val postDeclarationBlock = JsBlock()

        statements += preDeclarationBlock

        module.files.forEach {
            val fileStatements = it.accept(IrFileToJsTransformer(), context).statements
            if (fileStatements.isNotEmpty()) {
                statements.add(JsSingleLineComment("file: ${it.path}"))

                statements.addAll(fileStatements)
            }
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

    fun generateModule(module: IrModuleFragment): CompilerResult {
        val additionalPackages = with(backendContext) {
            externalPackageFragment.values + listOf(
                bodilessBuiltInsPackageFragment,
                intrinsics.externalPackageFragment
            ) + packageLevelJsModules
        }

        val exportedModule = ExportModelGenerator(backendContext).generateExport(module)
        val dts = exportedModule.toTypeScript()

        module.files.forEach { StaticMembersLowering(backendContext).lower(it) }

        namer.merge(module.files, additionalPackages)

        val program = JsProgram()

        val nameGenerator = IrNamerImpl(
            newNameTables = namer
        )
        val staticContext = JsStaticContext(
            backendContext = backendContext,
            irNamer = nameGenerator
        )
        val rootContext = JsGenerationContext(
            currentFunction = null,
            staticContext = staticContext
        )

        val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function")
        val internalModuleName = JsName("_")

        val (importStatements, importedJsModules) =
            generateImportStatements(
                getNameForExternalDeclaration = { rootContext.getNameForStaticDeclaration(it) },
                declareFreshGlobal = { JsName(sanitizeName(it)) } // TODO: Declare fresh name
            )

        val moduleBody = generateModuleBody(module, rootContext)
        val exportStatements = ExportModelToJsStatements(internalModuleName, namer)
            .generateModuleExport(exportedModule)

        if (generateScriptModule) {
            with(program.globalBlock) {
                statements += importStatements
                statements += moduleBody
                statements += exportStatements
            }
        } else {
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
        }

        return CompilerResult(program.toString(), dts)
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
}