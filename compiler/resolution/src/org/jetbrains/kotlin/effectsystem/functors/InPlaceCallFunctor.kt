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

package org.jetbrains.kotlin.effectsystem.functors

import org.jetbrains.kotlin.effectsystem.effects.ESCalls
import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.InvocationKind
import org.jetbrains.kotlin.effectsystem.factories.boundSchemaFromClauses
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.structure.*

class InPlaceCallFunctor(val invocationKind: InvocationKind, private val relevantParametersMask: List<Boolean>) : ESFunctor {
    override fun apply(arguments: List<EffectSchema>): EffectSchema? {
        assert(arguments.size == relevantParametersMask.size) {
            "InPlaceCallFunctor functor expects ${relevantParametersMask.size} arguments, got ${arguments.size}"
        }

        // Filter input arguments using mask
        val callableProvider = arguments.zip(relevantParametersMask).single { it.second }.first

        return boundSchemaFromClauses(callableProvider.clauses.map { clause ->
            val outcome = clause.effect as? ESReturns ?: return@map clause

            // If cast fails, it means that something is wrong with types
            val variable = outcome.value as? ESVariable ?: return null

            val newEffect = ESCalls(variable, invocationKind)
            return@map clause.replaceEffect(newEffect)
        })
    }
}