/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.utils.DFS

class IrModuleToJsTransformer(private val backendContext: JsIrBackendContext) : BaseIrElementToJsNodeTransformer<JsNode, Nothing?> {
    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): JsNode {
        val program = JsProgram()
        val rootContext = JsGenerationContext(JsRootScope(program), backendContext)

        // TODO: fix it up with new name generator
        val anyName = rootContext.getNameForSymbol(backendContext.irBuiltIns.anyClass)
        val throwableName = rootContext.getNameForSymbol(backendContext.irBuiltIns.throwableClass)

        program.globalBlock.statements += JsVars(JsVars.JsVar(anyName, Namer.JS_OBJECT))
        program.globalBlock.statements += JsVars(JsVars.JsVar(throwableName, Namer.JS_ERROR))

        val preDeclarationBlock = JsGlobalBlock()
        val postDeclarationBlock = JsGlobalBlock()

        program.globalBlock.statements += preDeclarationBlock

        declaration.files.forEach {
            program.globalBlock.statements.add(it.accept(IrFileToJsTransformer(), rootContext))
        }

        // sort member forwarding code
        processClassModels(rootContext.staticContext.classModels, preDeclarationBlock, postDeclarationBlock)

        program.globalBlock.statements += postDeclarationBlock
        program.globalBlock.statements += rootContext.staticContext.initializerBlock

        return program
    }

    private fun processClassModels(
        classModelMap: Map<JsName, JsClassModel>,
        preDeclarationBlock: JsGlobalBlock,
        postDeclarationBlock: JsGlobalBlock
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