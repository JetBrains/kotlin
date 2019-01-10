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
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument

internal class PsiCallsEffectParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    contractParserDispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, contractParserDispatcher) {

    override fun tryParseEffect(expression: KtExpression): Collection<EffectDeclaration> {
        val resolvedCall = expression.getResolvedCall(callContext.bindingContext) ?: return emptyList()
        val descriptor = resolvedCall.resultingDescriptor

        if (!descriptor.isCallsInPlaceEffectDescriptor()) return emptyList()

        val lambda = contractParserDispatcher.parseVariable(resolvedCall.firstArgumentAsExpressionOrNull()) ?: return emptyList()

        val kindArgument = resolvedCall.valueArgumentsByIndex?.getOrNull(1)

        val kind = when (kindArgument) {
            is DefaultValueArgument -> InvocationKind.UNKNOWN
            is ExpressionValueArgument -> contractParserDispatcher.parseKind(kindArgument.valueArgument?.getArgumentExpression())
            else -> null
        }

        if (kind == null) {
            val reportOn = (kindArgument as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression() ?: expression
            collector.badDescription("unrecognized InvocationKind", reportOn)
            return emptyList()
        }

        return listOf(CallsEffectDeclaration(lambda, kind))
    }
}