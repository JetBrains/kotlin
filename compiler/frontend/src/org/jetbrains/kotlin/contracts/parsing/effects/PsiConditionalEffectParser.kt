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

import org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration
import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.parsing.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class PsiConditionalEffectParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    dispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, dispatcher) {
    override fun tryParseEffect(expression: KtExpression): EffectDeclaration? {
        val resolvedCall = expression.getResolvedCall(callContext.bindingContext) ?: return null
        if (!resolvedCall.resultingDescriptor.isImpliesCallDescriptor()) return null

        val effect = contractParserDispatcher.parseEffect(resolvedCall.dispatchReceiver.safeAs<ExpressionReceiver>()?.expression)
                ?: return null
        val condition = contractParserDispatcher.parseCondition(resolvedCall.firstArgumentAsExpressionOrNull())
                ?: return null

        return ConditionalEffectDeclaration(effect, condition)
    }
}