/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class CallableUsageReplacementStrategy(
    private val replacement: CodeToInline,
    private val inlineSetter: Boolean = false
) : UsageReplacementStrategy {
    override fun createReplacer(usage: KtSimpleNameExpression): (() -> KtElement?)? {
        val bindingContext = usage.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        val resolvedCall = usage.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.status.isSuccess) return null

        val callElement = when (resolvedCall) {
            is VariableAsFunctionResolvedCall -> resolvedCall.variableCall.call.callElement
            else -> resolvedCall.call.callElement

        }

        if (callElement !is KtExpression && callElement !is KtAnnotationEntry) {
            return null
        }

        //TODO: precheck pattern correctness for annotation entry

        return {
            if (usage is KtOperationReferenceExpression && usage.getReferencedNameElementType() != KtTokens.IDENTIFIER) {
                val nameExpression = OperatorToFunctionIntention.convert(usage.parent as KtExpression).second
                createReplacer(nameExpression)?.invoke()
            } else {
                CodeInliner(usage, bindingContext, resolvedCall, callElement, inlineSetter, replacement).doInline()
            }
        }
    }
}

