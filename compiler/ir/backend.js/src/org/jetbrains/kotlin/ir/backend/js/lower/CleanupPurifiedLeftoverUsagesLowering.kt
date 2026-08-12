/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.isLeftoverAfterObjectPurification
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Removes usages marked for removal in [PurifyObjectInstanceGettersLowering].
 *
 * Usages of `static_init` prevents declaration to be removed by DCE, so the lowering removes relevant call statements to reduce the
 * resulting bundle size.
 */
abstract class CleanupPurifiedLeftoverUsagesLowering(val context: JsCommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.isLeftoverAfterObjectPurification) {
                    return IrCompositeImpl(expression.startOffset, expression.endOffset, expression.type)
                }
                return super.visitCall(expression)
            }
        })
    }
}
