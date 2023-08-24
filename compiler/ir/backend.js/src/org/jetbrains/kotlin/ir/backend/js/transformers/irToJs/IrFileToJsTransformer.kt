/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsStaticContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsCompositeBlock

class IrFileToJsTransformer(private val useBareParameterNames: Boolean = false) : BaseIrElementToJsNodeTransformer<JsBlock, JsStaticContext> {
    override fun visitFile(declaration: IrFile, data: JsStaticContext): JsBlock {
        val fileContext = JsGenerationContext(
            currentFile = declaration,
            currentFunction = null,
            staticContext = data,
            useBareParameterNames = useBareParameterNames
        )
        val block = JsCompositeBlock()

        declaration.declarations.forEach {
            block.statements.add(it.accept(IrDeclarationToJsTransformer(), fileContext))
        }

        return block
    }
}
