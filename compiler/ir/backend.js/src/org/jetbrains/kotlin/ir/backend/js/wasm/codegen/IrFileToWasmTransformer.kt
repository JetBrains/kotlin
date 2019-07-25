/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm.codegen

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.js.backend.ast.JsBlock

class IrFileToWasmTransformer : BaseIrElementToWasmNodeTransformer<JsBlock, WasmStaticContext> {
    override fun visitFile(declaration: IrFile, data: WasmStaticContext): JsBlock {
        val block = JsBlock()

        declaration.declarations.forEach {
            block.statements.add(it.accept(IrDeclarationToWasmTransformer(), data))
        }

        return block
    }
}