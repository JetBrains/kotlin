/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.backend.ast.JsRootScope

class IrModuleToJsTransformer(val backendContext: JsIrBackendContext) : BaseIrElementToJsNodeTransformer<JsNode, Nothing?> {
    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): JsNode {
        val program = JsProgram()
        val rootContext = JsGenerationContext(JsRootScope(program), backendContext)

        declaration.files.forEach {
            program.globalBlock.statements.add(it.accept(IrFileToJsTransformer(), rootContext))
        }

        return program
    }
}