/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsGlobalBlock

class IrFileToJsTransformer : BaseIrElementToJsNodeTransformer<JsBlock, JsGenerationContext> {
    override fun visitFile(declaration: IrFile, data: JsGenerationContext): JsBlock {
        val fileContext = data.newDeclaration()
        val block = JsGlobalBlock()

        declaration.declarations.forEach {
            block.statements.add(it.accept(IrDeclarationToJsTransformer(), fileContext))
        }

        return block
    }
}