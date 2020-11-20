/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Move initialization of global fields to start function.
 *
 * WebAssembly allows only constant expressions to be used directly in
 * field initializers.
 *
 * TODO: Don't move constant expression initializers
 * TODO: Make field initialization lazy. Needs design.
 */
class FieldInitializersLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val builder = context.createIrBuilder(context.startFunction.symbol)
        val startFunctionBody = context.startFunction.body as IrBlockBody

        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitField(declaration: IrField) {
                super.visitField(declaration)
                if (!declaration.isStatic) return
                val initValue: IrExpression = declaration.initializer?.expression ?: return

                startFunctionBody.statements.add(
                    builder.at(initValue).irSetField(null, declaration, initValue)
                )
                // Replace initializer with default one
                declaration.initializer = null
            }
        })
    }
}