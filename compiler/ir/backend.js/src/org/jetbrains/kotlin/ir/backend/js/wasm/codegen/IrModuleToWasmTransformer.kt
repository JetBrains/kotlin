/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.backend.js.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

class IrModuleToWasmTransformer(
    private val backendContext: WasmBackendContext
) : BaseIrElementToWasmNodeTransformer<JsNode, Nothing?> {
    val moduleName = backendContext.configuration[CommonConfigurationKeys.MODULE_NAME]!!

    private fun generateModuleBody(module: IrModuleFragment, context: WasmStaticContext): List<JsStatement> {
        val statements = mutableListOf<JsStatement>(
            JsStringLiteral("use strict").makeStmt()
        )

        val preDeclarationBlock = JsBlock()
        val postDeclarationBlock = JsBlock()

        statements += preDeclarationBlock

        module.files.forEach {
            if (!it.declarations.isEmpty()) {
                statements.add(JsDocComment(mapOf("file" to it.path)).makeStmt())
                statements.addAll(it.accept(IrFileToWasmTransformer(), context).statements)
            }
        }

        // sort member forwarding code
        processClassModels(context.classModels, preDeclarationBlock, postDeclarationBlock)

        statements += postDeclarationBlock
        statements += context.initializerBlock

        return statements
    }

    private fun generateExportStatements(
        module: IrModuleFragment,
        context: WasmStaticContext,
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
        context: WasmStaticContext,
        internalModuleName: JsName
    ): JsExpressionStatement? {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration.visibility != Visibilities.PUBLIC) {
            return null
        }

        if (!declaration.isExported())
            return null

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
                val instanceGetter = backendContext.objectToGetInstanceFunction[declaration.symbol]!!
                val instanceGetterName: JsName = context.getNameForStaticFunction(instanceGetter)
                defineProperty(internalModuleName.makeRef(), name.ident, getter = JsNameRef(instanceGetterName))
            } else {
                jsAssignment(JsNameRef(exportName, internalModuleName.makeRef()), name.makeRef())
            }

        return JsExpressionStatement(expression)
    }

    private fun generateModule(module: IrModuleFragment): JsProgram {
        val additionalPackages = with(backendContext) {
            externalPackageFragment.values + listOf(
                // bodilessBuiltInsPackageFragment
            ) + packageLevelJsModules
        }

        val namer = NameTables(module.files + additionalPackages)

        val program = JsProgram()

        val nameGenerator = IrNamerImpl(
            newNameTables = namer
        )
        val staticContext = WasmStaticContext(
            irNamer = nameGenerator
        )

        val rootContext = staticContext

        val rootFunction = JsFunction(program.rootScope, JsBlock(), "root function")
        val internalModuleName = JsName("_")

        val (importStatements, importedJsModules) =
            generateImportStatements(
                getNameForExternalDeclaration = { staticContext.getNameForStaticDeclaration(it) },
                declareFreshGlobal = { JsName(sanitizeName(it)) } // TODO: Declare fresh name
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
                statements += JsReturn(internalModuleName.makeRef())
            }
        }

        program.globalBlock.statements += ModuleWrapperTranslation.wrap(
            moduleName,
            rootFunction,
            importedJsModules,
            program,
            kind = ModuleKind.PLAIN
        )

        return program
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

    fun IrDeclarationWithName.isExported(): Boolean {
        if (fqNameWhenAvailable in backendContext.additionalExportedDeclarations)
            return true

        // Hack to support properties
        val correspondingProperty = when {
            this is IrField -> correspondingPropertySymbol
            this is IrSimpleFunction -> correspondingPropertySymbol
            else -> null
        }
        correspondingProperty?.let {
            return it.owner.isExported()
        }

        if (isJsExport())
            return true

        return when (val parent = parent) {
            is IrDeclarationWithName -> parent.isExported()
            is IrAnnotationContainer -> parent.isJsExport()
            else -> false
        }
    }
}