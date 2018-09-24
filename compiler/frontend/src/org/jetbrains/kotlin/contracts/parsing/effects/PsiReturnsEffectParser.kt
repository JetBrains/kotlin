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

import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.description.ReturnsEffectDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.parsing.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

internal class PsiReturnsEffectParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    contractParserDispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, contractParserDispatcher) {
    override fun tryParseEffect(expression: KtExpression): EffectDeclaration? {
        val resolvedCall = expression.getResolvedCall(callContext.bindingContext) ?: return null
        val descriptor = resolvedCall.resultingDescriptor

        if (descriptor.isReturnsNotNullDescriptor()) return ReturnsEffectDeclaration(ConstantReference.NOT_NULL)
        if (descriptor.isReturnsWildcardDescriptor()) return ReturnsEffectDeclaration(ConstantReference.WILDCARD)

        if (!descriptor.isReturnsEffectDescriptor()) return null

        val argumentExpression = resolvedCall.firstArgumentAsExpressionOrNull()
        val constantValue = if (argumentExpression != null) contractParserDispatcher.parseConstant(argumentExpression) else null

        if (constantValue == null) {
            collector.badDescription(
                "only true/false/null constants in Returns-effect are currently supported",
                argumentExpression ?: expression
            )
            return null
        }

        return ReturnsEffectDeclaration(constantValue)
    }
}