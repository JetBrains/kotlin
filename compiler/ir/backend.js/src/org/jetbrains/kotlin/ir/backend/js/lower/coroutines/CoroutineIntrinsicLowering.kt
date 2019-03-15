/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.isBuiltInIntercepted
import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class CoroutineIntrinsicLowering(val context: JsIrBackendContext): FileLoweringPass {
    private val languageVersion = context.configuration.languageVersionSettings
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)
                return when {
                    expression.descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersion) ->
                        irCall(expression, context.coroutineSuspendOrReturn)
                    expression.descriptor.isBuiltInIntercepted(languageVersion) ->
                        error("Intercepted should not be used with release coroutines")
                    expression.symbol == context.intrinsics.jsCoroutineContext ->
                        irCall(expression, context.coroutineGetContextJs)
                    else -> call
                }
            }
        })
    }
}
