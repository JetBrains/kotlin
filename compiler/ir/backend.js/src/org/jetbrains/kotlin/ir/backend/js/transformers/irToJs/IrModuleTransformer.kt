/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsProgram

class IrModuleTransformer : IrElementToJsNodeTransformer<JsNode, Nothing?> {
    override fun visitModuleFragment(declaration: IrModuleFragment, data: Nothing?): JsNode {
        val program = JsProgram()

        declaration.files.forEach {
            program.globalBlock.statements.add(it.accept(IrFileTransformer(), null))
        }

        return program
    }
}