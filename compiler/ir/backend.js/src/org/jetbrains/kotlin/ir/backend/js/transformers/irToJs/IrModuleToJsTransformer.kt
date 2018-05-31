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

class IrModuleToJsTransformer(val backendContext: JsIrBackendContext) : BaseIrElementToJsNodeTransformer<JsNode, Nothing?> {
    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): JsNode {
        val program = JsProgram()
        val rootContext = JsGenerationContext(JsRootScope(program), backendContext)

        // TODO: fix it up with new name generator
        val kotlinAny = "kotlin\$${backendContext.builtIns.any.name.asString()}"
        val anyName = rootContext.currentScope.declareName(kotlinAny)

        program.globalBlock.statements += JsVars(JsVars.JsVar(anyName, Namer.JS_OBJECT))

        declaration.files.forEach {
            program.globalBlock.statements.add(it.accept(IrFileToJsTransformer(), rootContext))
        }

        // sort member forwarding code
        program.globalBlock.statements += addPostDeclarations(rootContext.staticContext.classModels)

        return program
    }


    private fun addPostDeclarations(classModels: Map<JsName, JsClassModel>): List<JsStatement> {

        val statements = mutableListOf<JsStatement>()
        val visited = mutableSetOf<JsName>()

        for (name in classModels.keys) {
            addPostDeclaration(name, visited, statements, classModels)
        }

        return statements
    }

    private fun addPostDeclaration(
        name: JsName,
        visited: MutableSet<JsName>,
        statements: MutableList<JsStatement>,
        classModels: Map<JsName, JsClassModel>
    ) {
        if (visited.add(name)) {
            classModels[name]?.run {
                superName?.let { addPostDeclaration(it, visited, statements, classModels) }
                interfaces.forEach { addPostDeclaration(it, visited, statements, classModels) }

                statements += postDeclarationBlock.statements
            }
        }
    }
}