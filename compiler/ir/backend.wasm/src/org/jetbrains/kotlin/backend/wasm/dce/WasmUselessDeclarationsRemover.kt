/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dce

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class WasmUselessDeclarationsRemover(
    private val context: WasmBackendContext,
    private val usefulDeclarations: Set<IrDeclaration>
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        process(declaration)
    }

    override fun visitClass(declaration: IrClass) {
        process(declaration)
    }

    // TODO bring back the primary constructor fix
    private fun process(container: IrDeclarationContainer) {
        container.declarations.transformFlat { member ->
            if (member !in usefulDeclarations) {
                emptyList()
            } else {
                member.acceptVoid(this)
                null
            }
        }
    }
}