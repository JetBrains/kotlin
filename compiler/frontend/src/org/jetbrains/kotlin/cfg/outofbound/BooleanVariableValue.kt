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

import org.jetbrains.kotlin.descriptors.VariableDescriptor

public sealed class BooleanVariableValue {
    // Logic operators, (BoolVariableValue, BoolVariableValue) -> BoolVariableValue
    public abstract fun and(other: BooleanVariableValue?): BooleanVariableValue
    public abstract fun or(other: BooleanVariableValue?): BooleanVariableValue
    public abstract fun not(): BooleanVariableValue

    // For now derived classes of BooleanVariableValue are immutable,
    // so copy returns this. In the future, if some class become mutable
    // the implementation of this method may change
    public fun copy(): BooleanVariableValue = this

    public object True : BooleanVariableValue() {
        override fun toString(): String = "T"

        override fun and(other: BooleanVariableValue?): BooleanVariableValue =
            when (other) {
                True -> True
                else -> other?.copy() ?: Undefined.WITH_NO_RESTRICTIONS
            }

        override fun or(other: BooleanVariableValue?): BooleanVariableValue = True

        override fun not(): BooleanVariableValue = False
    }

    public object False : BooleanVariableValue() {
        override fun toString(): String = "F"

        override fun and(other: BooleanVariableValue?): BooleanVariableValue = False

        override fun or(other: BooleanVariableValue?): BooleanVariableValue = other?.copy() ?: Undefined.WITH_NO_RESTRICTIONS

        override fun not(): BooleanVariableValue = True
    }

    public data class Undefined (
            val onTrueRestrictions: Restrictions,
            val onFalseRestrictions: Restrictions
    ): BooleanVariableValue() {
        override fun toString(): String = "U${onTrueRestrictions.toString()}${onFalseRestrictions.toString()}"

        override fun and(other: BooleanVariableValue?): BooleanVariableValue =
                when(other) {
                    null -> Undefined.WITH_NO_RESTRICTIONS
                    True -> this.copy()
                    False -> False
                    is Undefined -> Undefined(
                            this.onTrueRestrictions.andInTruePosition(other.onTrueRestrictions),
                            this.onFalseRestrictions.andInFalsePosition(other.onFalseRestrictions)
                    )
                }

        override fun or(other: BooleanVariableValue?): BooleanVariableValue =
                when(other) {
                    null -> Undefined.WITH_NO_RESTRICTIONS
                    True -> True
                    False -> this.copy()
                    is Undefined -> Undefined(
                            this.onTrueRestrictions.orInTruePosition(other.onTrueRestrictions),
                            this.onFalseRestrictions.orInFalsePosition(other.onFalseRestrictions)
                    )
                }

        override fun not(): BooleanVariableValue = Undefined(onFalseRestrictions, onTrueRestrictions)

        companion object {
            public val WITH_NO_RESTRICTIONS: Undefined = Undefined(Restrictions.Empty, Restrictions.Empty)
            public val WITH_FULL_RESTRICTIONS: Undefined = Undefined(Restrictions.Full, Restrictions.Full)
        }
    }

    companion object {
        public fun create(value: Boolean): BooleanVariableValue = if(value) True else False
    }
}

public sealed class Restrictions {
    public abstract fun andInTruePosition(other: Restrictions): Restrictions
    public abstract fun andInFalsePosition(other: Restrictions): Restrictions
    public abstract fun orInTruePosition(other: Restrictions): Restrictions
    public abstract fun orInFalsePosition(other: Restrictions): Restrictions

    public object Empty : Restrictions() {
        override fun toString(): String = "{}"

        override fun andInTruePosition(other: Restrictions): Restrictions = other

        override fun andInFalsePosition(other: Restrictions): Restrictions = Empty

        override fun orInTruePosition(other: Restrictions): Restrictions = andInFalsePosition(other)

        override fun orInFalsePosition(other: Restrictions): Restrictions = andInTruePosition(other)
    }
    public object Full : Restrictions() {
        override fun toString(): String = "{full}"

        override fun andInTruePosition(other: Restrictions): Restrictions = Full

        override fun andInFalsePosition(other: Restrictions): Restrictions = other

        override fun orInTruePosition(other: Restrictions): Restrictions = andInFalsePosition(other)

        override fun orInFalsePosition(other: Restrictions): Restrictions = andInTruePosition(other)
    }
    public data class Specific protected constructor(val values: Map<VariableDescriptor, Set<Int>>) : Restrictions() {
        override fun toString(): String {
            val descriptorToString: (VariableDescriptor) -> String = { it.name.asString() }
            val setToString: (Set<Int>) -> String = { it.sort().toString() }
            return MapUtils.mapToString(values, descriptorToString, descriptorToString, setToString)
        }

        override fun andInTruePosition(other: Restrictions): Restrictions =
                when (other) {
                    is Specific -> Specific(MapUtils.mergeMaps(this.values, other.values) { v1, v2 -> v1 intersect v2 })
                    Empty -> this
                    Full -> Full
                }

        override fun andInFalsePosition(other: Restrictions): Restrictions =
                when (other) {
                    is Specific -> Specific(MapUtils.mergeMaps(this.values, other.values) { v1, v2 -> v1 union v2 })
                    Empty -> Empty
                    Full -> this
                }

        override fun orInTruePosition(other: Restrictions): Restrictions = andInFalsePosition(other)

        override fun orInFalsePosition(other: Restrictions): Restrictions = andInTruePosition(other)

        companion object {
            public fun create(values: Map<VariableDescriptor, Set<Int>>): Restrictions =
                    if (values.isEmpty()) Empty else Specific(values)
        }
    }
}