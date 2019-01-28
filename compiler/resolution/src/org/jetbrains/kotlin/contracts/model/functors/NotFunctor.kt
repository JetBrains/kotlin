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

import org.jetbrains.kotlin.contracts.model.ConditionalEffect
import org.jetbrains.kotlin.contracts.model.structure.ESConstants
import org.jetbrains.kotlin.contracts.model.structure.ESReturns
import org.jetbrains.kotlin.contracts.model.structure.isFalse
import org.jetbrains.kotlin.contracts.model.structure.isTrue

class NotFunctor(constants: ESConstants) : AbstractUnaryFunctor(constants) {
    override fun invokeWithReturningEffects(list: List<ConditionalEffect>): List<ConditionalEffect> = list.mapNotNull {
        val outcome = it.simpleEffect

        // Outcome guaranteed to be Returns by AbstractSequentialUnaryFunctor, but value
        // can be non-boolean in case of type-errors in the whole expression, like "foo(bar) && 1"
        val returnValue = (outcome as ESReturns).value

        when {
            returnValue.isTrue -> ConditionalEffect(it.condition, ESReturns(constants.falseValue))
            returnValue.isFalse -> ConditionalEffect(it.condition, ESReturns(constants.trueValue))
            else -> null
        }
    }
}
