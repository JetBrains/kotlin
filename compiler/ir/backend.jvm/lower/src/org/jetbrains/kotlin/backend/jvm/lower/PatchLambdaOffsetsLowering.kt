/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * Patches lambda-originated function reference offsets.
 *
 * Suppose we have:
 *
 * // line 1    foo()
 * // line 2    {
 *                  ...
 *              }
 *
 *  We prefer debugger to suspend at line 1 when it reaches `foo` invocation, not the line 2.
 *  To achieve that, we simply set the corresponding [IrRichFunctionReference] offsets to the parent call.
 *  It is the same, when the lambda is being assigned to the variable.
 */
class PatchLambdaOffsetsLowering(@Suppress("UNUSED_PARAMETER", "unused") context: CommonBackendContext) :
    IrTransformer<PatchLambdaOffsetsLoweringContext>(),
    BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(this, PatchLambdaOffsetsLoweringContext())
    }

    override fun visitCall(expression: IrCall, data: PatchLambdaOffsetsLoweringContext) = super.visitCall(
        expression,
        PatchLambdaOffsetsLoweringContext(
            expression.startOffset,
            expression.endOffset
        )
    )

    override fun visitVariable(declaration: IrVariable, data: PatchLambdaOffsetsLoweringContext) = super.visitVariable(
        declaration,
        PatchLambdaOffsetsLoweringContext(
            declaration.startOffset,
            declaration.endOffset
        )
    )

    override fun visitContainerExpression(
        expression: IrContainerExpression,
        data: PatchLambdaOffsetsLoweringContext,
    ): IrExpression {
        if (expression !is IrReturnableBlock) return super.visitContainerExpression(expression, data)
        return super.visitContainerExpression(expression, PatchLambdaOffsetsLoweringContext())
    }

    override fun visitDeclaration(declaration: IrDeclarationBase, data: PatchLambdaOffsetsLoweringContext): IrStatement {
        return super.visitDeclaration(declaration, PatchLambdaOffsetsLoweringContext())
    }

    override fun visitRichFunctionReference(
        expression: IrRichFunctionReference,
        data: PatchLambdaOffsetsLoweringContext,
    ): IrExpression {
        /** When a lambda is being SAM-converted, we'd like to suspend on the functional interface line,
         ** to indicate that it's being instantiated:
         *
         * // line 1    Runnable()      // <- suspend here
         * // line 2    {
         *                  ...
         *              }
         *
         *  The proper offset is already set by [org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences], we keep it.
         */
        if (expression.origin == IrStatementOrigin.LAMBDA && data.startOffset != null && !expression.isSamConversion()) {
            require(data.endOffset != null) { "end offset must not be null, as start offset was set" }
            expression.startOffset = data.startOffset
            expression.endOffset = data.endOffset
        }
        return super.visitRichFunctionReference(expression, data)
    }
}

class PatchLambdaOffsetsLoweringContext(
    val startOffset: Int? = null,
    val endOffset: Int? = null,
)