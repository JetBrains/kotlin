/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
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

fun provideArgumentNameHints(element: KtCallExpression): List<InlayInfo> {
    if (element.valueArguments.none { it.getArgumentExpression()?.isUnclearExpression() == true }) return emptyList()
    val ctx = element.analyze(BodyResolveMode.PARTIAL)
    val call = element.getCall(ctx) ?: return emptyList()
    val resolvedCall = call.getResolvedCall(ctx)
    if (resolvedCall != null) {
        return getParameterInfoForCallCandidate(resolvedCall)
    }
    val candidates = call.resolveCandidates(ctx, element.getResolutionFacade())
    if (candidates.isEmpty()) return emptyList()
    candidates.singleOrNull()?.let { return getParameterInfoForCallCandidate(it) }
    return candidates.map { getParameterInfoForCallCandidate(it) }.reduce { infos1, infos2 ->
        for (index in infos1.indices) {
            if (index >= infos2.size || infos1[index] != infos2[index]) {
                return@reduce infos1.subList(0, index)
            }
        }
        infos1
    }
}

private fun getParameterInfoForCallCandidate(resolvedCall: ResolvedCall<out CallableDescriptor>): List<InlayInfo> {
    return resolvedCall.valueArguments.mapNotNull { (valueParam: ValueParameterDescriptor, resolvedArg) ->
        resolvedArg.arguments.firstOrNull()?.let { arg ->
            arg.getArgumentExpression()?.let { argExp ->
                if (!arg.isNamed() && !valueParam.name.isSpecial && argExp.isUnclearExpression()) {
                    val prefix = if (valueParam.varargElementType != null) "..." else ""
                    return@mapNotNull InlayInfo(prefix + valueParam.name.identifier, argExp.startOffset)
                }
            }
        }
        null
    }
}

private fun KtExpression.isUnclearExpression() = when(this) {
    is KtConstantExpression, is KtThisExpression, is KtBinaryExpression, is KtStringTemplateExpression -> true
    is KtPrefixExpression -> baseExpression is KtConstantExpression && (operationToken == KtTokens.PLUS || operationToken == KtTokens.MINUS)
    else -> false
}
