/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg.outofbound

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.cfg.outofbound.IntegerVariableValues
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

public interface BooleanVariableValue {
    // Logic operators, (BoolVariableValue, BoolVariableValue) -> BoolVariableValue
    public fun and(other: BooleanVariableValue): BooleanVariableValue
    public fun or(other: BooleanVariableValue): BooleanVariableValue
    public fun not(): BooleanVariableValue

    public object True : BooleanVariableValue {
        override fun toString(): String = "T"

        override fun and(other: BooleanVariableValue): BooleanVariableValue =
            when (other) {
                True -> True
                else -> other.copy()
            }

        override fun or(other: BooleanVariableValue): BooleanVariableValue = True

        override fun not(): BooleanVariableValue = False
    }

    public object False : BooleanVariableValue {
        override fun toString(): String = "F"

        override fun and(other: BooleanVariableValue): BooleanVariableValue = False

        override fun or(other: BooleanVariableValue): BooleanVariableValue = other.copy()

        override fun not(): BooleanVariableValue = True
    }

    public data class Undefined (
            val onTrueRestrictions: Map<VariableDescriptor, Set<Int>>,
            val onFalseRestrictions: Map<VariableDescriptor, Set<Int>>
    ): BooleanVariableValue {
        override fun toString(): String {
            val descriptorToString: (VariableDescriptor) -> String = { it.getName().asString() }
            val setToString: (Set<Int>) -> String = { it.sort().toString() }
            val onTrue = MapUtils.mapToString(onTrueRestrictions, descriptorToString, descriptorToString, setToString)
            val onFalse = MapUtils.mapToString(onFalseRestrictions, descriptorToString, descriptorToString, setToString)
            return "U$onTrue$onFalse"
        }

        override fun and(other: BooleanVariableValue): BooleanVariableValue =
                when(other) {
                    True -> this.copy()
                    False -> False
                    is Undefined -> mergeCorrespondingValues(
                            other,
                            { value1, value2 -> value1.intersect(value2) },
                            { value1, value2 -> value1.union(value2) }
                    )
                    else -> {
                        assert(false, "Unexpected derived type of BooleanVariableValue")
                        BooleanVariableValue.undefinedWithNoRestrictions
                    }
                }

        override fun or(other: BooleanVariableValue): BooleanVariableValue =
                when(other) {
                    True -> True
                    False -> this.copy()
                    is Undefined -> mergeCorrespondingValues(
                            other,
                            { value1, value2 -> value1.union(value2) },
                            { value1, value2 -> value1.intersect(value2) }
                    )
                    else -> {
                        assert(false, "Unexpected derived type of BooleanVariableValue")
                        BooleanVariableValue.undefinedWithNoRestrictions
                    }
                }

        override fun not(): BooleanVariableValue = Undefined(onFalseRestrictions, onTrueRestrictions)

        private fun mergeCorrespondingValues(
                other: Undefined,
                mergeOnTrueValues: (Set<Int>, Set<Int>) -> Set<Int>,
                mergeOnFalseValues: (Set<Int>, Set<Int>) -> Set<Int>
        ): Undefined {
            val onTrueIntersected = MapUtils.mergeMaps(onTrueRestrictions, other.onTrueRestrictions, mergeOnTrueValues)
            val onFalseIntersected = MapUtils.mergeMaps(onFalseRestrictions, other.onFalseRestrictions, mergeOnFalseValues)
            return Undefined(onTrueIntersected, onFalseIntersected)
        }
    }

    // For now derived classes of BooleanVariableValue are immutable,
    // so copy returns this. In the future, if some class become mutable
    // the implementation of this method may change
    public fun copy(): BooleanVariableValue = this

    companion object {
        public val undefinedWithNoRestrictions: Undefined = Undefined(mapOf(), mapOf())
        public fun create(value: Boolean): BooleanVariableValue = if(value) True else False
    }
}