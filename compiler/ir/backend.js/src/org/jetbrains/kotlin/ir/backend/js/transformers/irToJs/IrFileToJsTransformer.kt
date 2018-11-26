/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.js.backend.ast.JsDeclarationScope
import org.jetbrains.kotlin.js.backend.ast.JsStatement

class IrFileToJsTransformer : BaseIrElementToJsNodeTransformer<JsStatement, JsGenerationContext> {
    override fun visitFile(declaration: IrFile, context: JsGenerationContext): JsStatement {
        val fileContext = context.newDeclaration(JsDeclarationScope(context.currentScope, "scope for file ${declaration.path}"))
        val block = fileContext.currentBlock

        declaration.declarations.forEach {
            block.statements.add(it.accept(IrDeclarationToJsTransformer(), fileContext))
        }

        return block
    }
}