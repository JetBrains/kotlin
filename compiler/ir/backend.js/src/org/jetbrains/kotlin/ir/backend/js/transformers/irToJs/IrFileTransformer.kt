/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class IrFileTransformer : IrElementToJsNodeTransformer<JsStatement, Nothing?> {
    override fun visitFile(declaration: IrFile, data: Nothing?): JsStatement {
        val block = JsBlock()

        declaration.declarations.forEach {
            block.statements.add(it.accept(IrDeclarationToJsTransformer(), null))
        }

        return block
    }
}