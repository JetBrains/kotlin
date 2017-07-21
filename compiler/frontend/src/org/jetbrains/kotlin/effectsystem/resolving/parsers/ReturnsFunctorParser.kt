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

package org.jetbrains.kotlin.effectsystem.resolving.parsers

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.factories.UNKNOWN_CONSTANT
import org.jetbrains.kotlin.effectsystem.factories.singleClauseSchema
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.resolving.FunctorParser
import org.jetbrains.kotlin.effectsystem.resolving.RETURNS_EFFECT
import org.jetbrains.kotlin.effectsystem.resolving.utility.UtilityParsers
import org.jetbrains.kotlin.effectsystem.resolving.utility.extensionReceiverToESVariable
import org.jetbrains.kotlin.effectsystem.resolving.utility.toESConstant
import org.jetbrains.kotlin.effectsystem.resolving.utility.toESVariable
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class ReturnsFunctorParser : FunctorParser {
    override fun tryParseFunctor(resolvedCall: ResolvedCall<*>): ESFunctor? {
        val condition = UtilityParsers.conditionParser.parseCondition(resolvedCall) ?: return null

        val returnsAnnotation = resolvedCall.resultingDescriptor.annotations.findAnnotation(RETURNS_EFFECT) ?: return null
        val returnsArg = returnsAnnotation.allValueArguments.values.singleOrNull()?.toESConstant() ?: UNKNOWN_CONSTANT

        return singleClauseSchema(condition, ESReturns(returnsArg), getParameters(resolvedCall))
    }

    private fun getParameters(resolvedCall: ResolvedCall<*>): List<ESVariable> {
        val allParameters = mutableListOf<ESVariable>()
        resolvedCall.resultingDescriptor.extensionReceiverParameter?.extensionReceiverToESVariable()?.let { allParameters += it }
        resolvedCall.resultingDescriptor.valueParameters.mapTo(allParameters) { it.toESVariable() }
        return allParameters
    }
}