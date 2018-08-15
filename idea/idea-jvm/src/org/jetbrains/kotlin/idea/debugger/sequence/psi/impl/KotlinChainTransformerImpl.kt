// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.impl

import com.intellij.debugger.streams.psi.ChainTransformer
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.QualifierExpression
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.impl.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.KotlinPsiUtil
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class KotlinChainTransformerImpl(private val typeExtractor: CallTypeExtractor) : ChainTransformer<KtCallExpression> {
    override fun transform(callChain: List<KtCallExpression>, context: PsiElement): StreamChain {
        val intermediateCalls = mutableListOf<IntermediateStreamCall>()
        for (call in callChain.subList(0, callChain.size - 1)) {
            val (typeBefore, typeAfter) = typeExtractor.extractIntermediateCallTypes(call)
            intermediateCalls += IntermediateStreamCallImpl(
                call.callName(), call.valueArguments.map { createCallArgument(call, it) },
                typeBefore, typeAfter,
                call.textRange
            )
        }

        val terminationsPsiCall = callChain.last()
        val (typeBeforeTerminator, resultType) = typeExtractor.extractTerminalCallTypes(terminationsPsiCall)
        val terminationCall = TerminatorStreamCallImpl(
            terminationsPsiCall.callName(),
            terminationsPsiCall.valueArguments.map { createCallArgument(terminationsPsiCall, it) },
            typeBeforeTerminator, resultType, terminationsPsiCall.textRange
        )

        val typeAfterQualifier =
            if (intermediateCalls.isEmpty()) typeBeforeTerminator else intermediateCalls.first().typeBefore

        val qualifier = createQualifier(callChain.first(), typeAfterQualifier)

        return StreamChainImpl(qualifier, intermediateCalls, terminationCall, context)
    }

    private fun createCallArgument(callExpression: KtCallExpression, arg: KtValueArgument): CallArgument {
        fun KtValueArgument.toCallArgument(): CallArgument {
            val argExpression = getArgumentExpression()!!
            return CallArgumentImpl(KotlinPsiUtil.getTypeName(argExpression.resolveType()), this.text)
        }

        val bindingContext = callExpression.getResolutionFacade().analyzeWithAllCompilerChecks(listOf(callExpression)).bindingContext
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return arg.toCallArgument()
        val parameter = resolvedCall.getParameterForArgument(arg) ?: return arg.toCallArgument()
        return CallArgumentImpl(KotlinPsiUtil.getTypeName(parameter.type), arg.text)
    }

    private fun createQualifier(expression: PsiElement, typeAfter: GenericType): QualifierExpression {
        val parent = expression.parent as? KtDotQualifiedExpression
                ?: return QualifierExpressionImpl("", TextRange.EMPTY_RANGE, typeAfter)
        val receiver = parent.receiverExpression

        return QualifierExpressionImpl(receiver.text, receiver.textRange, typeAfter)
    }
}
