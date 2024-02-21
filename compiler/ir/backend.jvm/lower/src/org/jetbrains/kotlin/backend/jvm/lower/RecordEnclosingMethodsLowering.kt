/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.ir.unwrapInlineLambda
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

@PhaseDescription(
    name = "RecordEnclosingMethods",
    description = "Find enclosing methods for objects inside inline and dynamic lambdas"
)
internal class RecordEnclosingMethodsLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.accept(object : IrElementVisitor<Unit, IrFunction?> {
            override fun visitElement(element: IrElement, data: IrFunction?) =
                element.acceptChildren(this, element as? IrFunction ?: data)

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrFunction?) {
                require(data != null) { "function call not in a method: ${expression.render()}" }
                when {
                    expression.symbol == context.ir.symbols.indyLambdaMetafactoryIntrinsic -> {
                        val reference = expression.getValueArgument(1)
                        if (reference is IrFunctionReference && reference.origin.isLambda) {
                            recordEnclosingMethodOverride(reference.symbol.owner, data)
                        }
                    }
                    expression.symbol.owner.isInlineFunctionCall(context) -> {
                        for (parameter in expression.symbol.owner.valueParameters) {
                            val lambda = expression.getValueArgument(parameter.index)?.unwrapInlineLambda() ?: continue
                            recordEnclosingMethodOverride(lambda.symbol.owner, data)
                        }
                    }
                }
                return super.visitFunctionAccess(expression, data)
            }

            private fun recordEnclosingMethodOverride(from: IrFunction, to: IrFunction) =
                context.enclosingMethodOverride.merge(from, to) { old, new ->
                    // A single lambda can be referenced multiple times if it is in a field initializer
                    // or an anonymous initializer block and there are multiple non-delegating constructors.
                    assert(old.parentAsClass == new.parentAsClass && old is IrConstructor && new is IrConstructor)
                    old.parentAsClass.primaryConstructor ?: old
                }
        }, null)
}
