/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_EMIT_DESCRIPTOR
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_FUNCTION_DESCRIPTOR
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.CallGenerator
import org.jetbrains.kotlin.psi2ir.generators.ErrorExpressionGenerator
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.psi2ir.generators.generateCall
import org.jetbrains.kotlin.psi2ir.generators.getResolvedCall
import org.jetbrains.kotlin.psi2ir.generators.pregenerateCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.util.OperatorNameConventions

class ComposeSyntheticIrExtension : SyntheticIrExtension {

    private fun StatementGenerator.visitCallExpressionWithoutInterception(
        expression: KtCallExpression
    ): IrExpression {
        val resolvedCall = getResolvedCall(expression)
            ?: return ErrorExpressionGenerator(this)
                .generateErrorCall(expression)

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            val functionCall = pregenerateCall(resolvedCall.functionCall)
            return CallGenerator(this)
                .generateCall(expression, functionCall, IrStatementOrigin.INVOKE)
        }

        val calleeExpression = expression.calleeExpression
        val origin =
            if (resolvedCall.resultingDescriptor.name == OperatorNameConventions.INVOKE &&
                calleeExpression !is KtSimpleNameExpression &&
                calleeExpression !is KtQualifiedExpression
            )
                IrStatementOrigin.INVOKE
            else
                null

        return CallGenerator(this).generateCall(
            expression.startOffsetSkippingComments,
            expression.endOffset,
            pregenerateCall(resolvedCall),
            origin
        )
    }

    val ParameterDescriptor.containingFunction: FunctionDescriptor get() =
        (this as DeclarationDescriptor).containingDeclaration as FunctionDescriptor

    override fun visitCallExpression(
        statementGenerator: StatementGenerator,
        element: KtCallExpression
    ): IrExpression? {
        val resolvedCall = statementGenerator.getResolvedCall(element)
            ?: return ErrorExpressionGenerator(statementGenerator).generateErrorCall(element)

        val descriptor = resolvedCall.candidateDescriptor

        if (descriptor is ComposableFunctionDescriptor) {
            if (descriptor.isInline) {
                return statementGenerator.visitCallExpressionWithoutInterception(element)
            } else if (resolvedCall is VariableAsFunctionResolvedCall) {
                val param = resolvedCall.variableCall.candidateDescriptor
                if (
                    param is ParameterDescriptor &&
                    param.containingFunction.isInline &&
                    InlineUtil.isInlineParameter(param)
                ) {
                    return statementGenerator.visitCallExpressionWithoutInterception(element)
                }
            }
            return statementGenerator.visitComposableCall(descriptor, element)
        }
        if (descriptor is ComposableEmitDescriptor) {
            return statementGenerator.visitEmitCall(descriptor, element)
        }

        return null
    }

    private fun StatementGenerator.visitEmitCall(
        descriptor: ComposableEmitDescriptor,
        expression: KtCallExpression
    ): IrExpression? {
        // NOTE(lmr):
        // we insert a block here with two calls:
        //  1. a call to get the composer
        //  2. the original call that would have been generated to the synthetic emit descriptor
        //
        // We need to do this here because this is the only place we can properly generate a
        // resolved call in full generality, which we need to do for the composer. We then intercept
        // this block in the lowering phase and generate the final code we want.
        return IrBlockImpl(
            expression.startOffset,
            expression.endOffset,
            descriptor.returnType?.toIrType() ?: context.irBuiltIns.unitType,
            COMPOSABLE_EMIT_OR_CALL,
            listOf(
                CallGenerator(this).generateCall(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    pregenerateCall(descriptor.composer),
                    COMPOSABLE_EMIT_OR_CALL
                ),
                visitCallExpressionWithoutInterception(expression)
            )
        ).also {
            context.irTrace.record(COMPOSABLE_EMIT_DESCRIPTOR, it, descriptor)
        }
    }

    private fun StatementGenerator.visitComposableCall(
        descriptor: ComposableFunctionDescriptor,
        expression: KtCallExpression
    ): IrExpression? {
        // NOTE(lmr):
        // we insert a block here with two calls:
        //  1. a call to get the composer
        //  2. the original call that would have been generated to the synthetic emit descriptor
        //
        // We need to do this here because this is the only place we can properly generate a
        // resolved call in full generality, which we need to do for the composer. We then intercept
        // this block in the lowering phase and generate the final code we want.
        return IrBlockImpl(
            expression.startOffset,
            expression.endOffset,
            descriptor.returnType?.toIrType() ?: context.irBuiltIns.unitType,
            COMPOSABLE_EMIT_OR_CALL,
            listOf(
                CallGenerator(this).generateCall(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    pregenerateCall(descriptor.composerCall),
                    COMPOSABLE_EMIT_OR_CALL
                ),
                visitCallExpressionWithoutInterception(expression)
            )
        ).also {
            context.irTrace.record(COMPOSABLE_FUNCTION_DESCRIPTOR, it, descriptor)
        }
    }
}

internal fun getKeyValue(descriptor: DeclarationDescriptor, startOffset: Int): Int =
    descriptor.fqNameSafe.toString().hashCode() xor startOffset

internal val COMPOSABLE_EMIT_OR_CALL =
    object : IrStatementOriginImpl("Composable Emit Or Call") {}