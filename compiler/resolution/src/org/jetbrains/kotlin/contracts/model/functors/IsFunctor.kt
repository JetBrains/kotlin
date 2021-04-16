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

package org.jetbrains.kotlin.contracts.model.functors

import org.jetbrains.kotlin.contracts.model.*
import org.jetbrains.kotlin.contracts.model.structure.*
import org.jetbrains.kotlin.contracts.model.visitors.Reducer

class IsFunctor(val type: ESType, val isNegated: Boolean) : AbstractFunctor() {
    override fun doInvocation(arguments: List<Computation>, typeSubstitution: ESTypeSubstitution, reducer: Reducer): List<ESEffect> {
        assert(arguments.size == 1) { "Wrong size of arguments list for Unary operator: expected 1, got ${arguments.size}" }
        return invokeWithArguments(arguments[0], typeSubstitution)
    }

    fun invokeWithArguments(arg: Computation, typeSubstitution: ESTypeSubstitution): List<ESEffect> {
        return if (arg is ESValue)
            invokeWithValue(arg, typeSubstitution)
        else
            emptyList()
    }

    private fun invokeWithValue(value: ESValue, typeSubstitution: ESTypeSubstitution): List<ConditionalEffect> {
        val substitutedKotlinType = typeSubstitution.substitutor.safeSubstitute(type.toKotlinType(typeSubstitution.builtIns).unwrap())

        val substitutedType = ESKotlinType(substitutedKotlinType)

        val trueIs = ESIs(value, IsFunctor(substitutedType, isNegated))
        val falseIs = ESIs(value, IsFunctor(substitutedType, isNegated.not()))

        val trueResult = ConditionalEffect(trueIs, ESReturns(ESConstants.trueValue))
        val falseResult = ConditionalEffect(falseIs, ESReturns(ESConstants.falseValue))
        return listOf(trueResult, falseResult)
    }
}
