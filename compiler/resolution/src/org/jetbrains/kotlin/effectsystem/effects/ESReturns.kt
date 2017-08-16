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

package org.jetbrains.kotlin.effectsystem.effects

import org.jetbrains.kotlin.effectsystem.factories.UNKNOWN_CONSTANT
import org.jetbrains.kotlin.effectsystem.structure.ESEffect
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.impls.ESVariable

data class ESReturns(val value: ESValue): ESEffect {
    override fun isImplies(other: ESEffect): Boolean? {
        if (other is ESThrows) return false

        if (other !is ESReturns) return null

        // ESReturns(x) implies ESReturns(?) for any 'x'
        if (other.value == UNKNOWN_CONSTANT) return true

        if (value is ESVariable || other.value is ESVariable) {
            // If at least one of values is Variable, then `this` definitely
            // implies `other` iff they are one and the same Variable, i.e.
            // their id's are equal.
            // Otherwise, result is unknown
            return if (value.id == other.value.id) true else null
        }

        // If neither of values are Variable, then both should be Constant - compare them by ids
        assert(value is ESConstant && other.value is ESConstant) {
            "ESValue which isn't ESVariable should be ESConstant, got $value and ${other.value}"
        }
        return value.id == other.value.id
    }
}