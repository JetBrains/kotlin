/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.resolveCandidates
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun provideArgumentNameHints(element: KtCallElement): List<InlayInfo> {
    if (element.valueArguments.none { it.getArgumentExpression()?.isUnclearExpression() == true }) return emptyList()
    val ctx = element.analyze(BodyResolveMode.PARTIAL)
    val call = element.getCall(ctx) ?: return emptyList()
    val resolvedCall = call.getResolvedCall(ctx)
    if (resolvedCall != null) {
        return getArgumentNameHintsForCallCandidate(resolvedCall, call.valueArgumentList)
    }
    val candidates = call.resolveCandidates(ctx, element.getResolutionFacade())
    if (candidates.isEmpty()) return emptyList()
    candidates.singleOrNull()?.let { return getArgumentNameHintsForCallCandidate(it, call.valueArgumentList) }
    return candidates.map { getArgumentNameHintsForCallCandidate(it, call.valueArgumentList) }.reduce { infos1, infos2 ->
        for (index in infos1.indices) {
            if (index >= infos2.size || infos1[index] != infos2[index]) {
                return@reduce infos1.subList(0, index)
            }
        }
        infos1
    }
}

private fun getArgumentNameHintsForCallCandidate(
    resolvedCall: ResolvedCall<out CallableDescriptor>,
    valueArgumentList: KtValueArgumentList?
): List<InlayInfo> {
    val resultingDescriptor = resolvedCall.resultingDescriptor
    if (resultingDescriptor.hasSynthesizedParameterNames() && resultingDescriptor !is FunctionInvokeDescriptor) {
        return emptyList()
    }

    return resolvedCall.valueArguments.mapNotNull { (valueParam: ValueParameterDescriptor, resolvedArg) ->
        if (resultingDescriptor is FunctionInvokeDescriptor &&
            valueParam.type.extractParameterNameFromFunctionTypeArgument() == null
        ) {
            return@mapNotNull null
        }

        resolvedArg.arguments.firstOrNull()?.let { arg ->
            arg.getArgumentExpression()?.let { argExp ->
                if (!arg.isNamed() && !valueParam.name.isSpecial && argExp.isUnclearExpression()) {
                    val prefix = if (valueParam.varargElementType != null) "..." else ""
                    val offset = if (arg == valueArgumentList?.arguments?.firstOrNull() && valueParam.varargElementType != null)
                        valueArgumentList.leftParenthesis?.textRange?.endOffset ?: argExp.startOffset
                    else
                        arg.getSpreadElement()?.startOffset ?: argExp.startOffset
                    return@mapNotNull InlayInfo(prefix + valueParam.name.identifier, offset)
                }
            }
        }
        null
    }
}

private fun KtExpression.isUnclearExpression() = when (this) {
    is KtConstantExpression, is KtThisExpression, is KtBinaryExpression, is KtStringTemplateExpression -> true
    is KtPrefixExpression -> baseExpression is KtConstantExpression && (operationToken == KtTokens.PLUS || operationToken == KtTokens.MINUS)
    else -> false
}
