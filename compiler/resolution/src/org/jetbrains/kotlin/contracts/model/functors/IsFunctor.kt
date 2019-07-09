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
import org.jetbrains.kotlin.contracts.model.structure.ESConstants
import org.jetbrains.kotlin.contracts.model.structure.ESIs
import org.jetbrains.kotlin.contracts.model.structure.ESReturns
import org.jetbrains.kotlin.contracts.model.structure.ESType
import org.jetbrains.kotlin.contracts.model.visitors.Reducer

class IsFunctor(val type: ESType, val isNegated: Boolean) : AbstractFunctor() {
    override fun doInvocation(arguments: List<Computation>, reducer: Reducer): List<ESEffect> {
        assert(arguments.size == 1) { "Wrong size of arguments list for Unary operator: expected 1, got ${arguments.size}" }
        return invokeWithArguments(arguments[0])
    }

    fun invokeWithArguments(arg: Computation): List<ESEffect> {
        return if (arg is ESValue)
            invokeWithValue(arg)
        else
            emptyList()
    }

    private fun invokeWithValue(value: ESValue): List<ConditionalEffect> {
        val trueIs = ESIs(value, this)
        val falseIs = ESIs(value, IsFunctor(type, isNegated.not()))

        val trueResult = ConditionalEffect(trueIs, ESReturns(ESConstants.trueValue))
        val falseResult = ConditionalEffect(falseIs, ESReturns(ESConstants.falseValue))
        return listOf(trueResult, falseResult)
    }
}
