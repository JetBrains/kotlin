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

package org.jetbrains.kotlin.contracts.parsing.effects

import org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration
import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.contracts.parsing.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents

internal class PsiCallsEffectParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    contractParserDispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, contractParserDispatcher) {

    override fun tryParseEffect(expression: KtExpression): EffectDeclaration? {
        val resolvedCall = expression.getResolvedCall(callContext.bindingContext) ?: return null
        val descriptor = resolvedCall.resultingDescriptor

        if (!descriptor.isCallsInPlaceEffectDescriptor()) return null

        val lambda = contractParserDispatcher.parseVariable(resolvedCall.firstArgumentAsExpressionOrNull()) ?: return null

        val kindArgument = resolvedCall.valueArgumentsByIndex?.getOrNull(1)

        val kind = when (kindArgument) {
            is DefaultValueArgument -> InvocationKind.UNKNOWN
            is ExpressionValueArgument -> kindArgument.valueArgument?.getArgumentExpression()?.toInvocationKind(callContext.bindingContext)
            else -> null
        }

        if (kind == null) {
            val reportOn = (kindArgument as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression() ?: expression
            collector.badDescription("unrecognized InvocationKind", reportOn)
            return null
        }

        return CallsEffectDeclaration(lambda, kind)
    }

    private fun KtExpression.toInvocationKind(bindingContext: BindingContext): InvocationKind? {
        val descriptor = this.getResolvedCall(bindingContext)?.resultingDescriptor ?: return null
        if (!descriptor.parents.first().isInvocationKindEnum()) return null

        return when (descriptor.fqNameSafe.shortName()) {
            ContractsDslNames.AT_MOST_ONCE_KIND -> InvocationKind.AT_MOST_ONCE
            ContractsDslNames.EXACTLY_ONCE_KIND -> InvocationKind.EXACTLY_ONCE
            ContractsDslNames.AT_LEAST_ONCE_KIND -> InvocationKind.AT_LEAST_ONCE
            ContractsDslNames.UNKNOWN_KIND -> InvocationKind.UNKNOWN
            else -> null
        }
    }
}