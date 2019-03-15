/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

val builtinRemovalLoweringPhase = makeIrFilePhase(
    ::BuiltinRemovalLowering,
    name = "BuiltinRemovalLowering",
    description = "Replace usages of builtins with actual library method calls"
)

class BuiltinRemovalLowering(val context: CommonBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                return if (expression.symbol == context.irBuiltIns.booleanNotSymbol) {
                    val booleanNotFunction = context.irBuiltIns.booleanClass.functions.find { it.owner.name.asString() == "not" }
                        ?: throw AssertionError("Boolean.not function not found")
                    irCall(expression, booleanNotFunction, dispatchReceiverAsFirstArgument = false, firstArgumentAsDispatchReceiver = true)
                } else {
                    expression
                }
            }
        })
    }
}
