/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// Replaces IrExpressionBody with IrBlockBody returning this expression.
// Taken from kotlin/native ReturnsInsertionLowering.kt

class ExpressionBodyTransformer(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                declaration.acceptChildrenVoid(this)

                context.createIrBuilder(declaration.symbol, declaration.endOffset, declaration.endOffset).run {
                    val body = declaration.body
                    if (body is IrExpressionBody)
                        declaration.body = context.irFactory.createBlockBody(body.startOffset, body.endOffset) {
                            statements += irReturn(body.expression)
                        }
                }
            }
        })
    }
}
