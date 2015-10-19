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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KtType

/**
 * This class describes an arbitrary object which has some value in data flow analysis.
 * In general case it's some r-value.
 */
class DataFlowValue(val id: Any?, val type: KtType, val kind: DataFlowValue.Kind, val immanentNullability: Nullability) {

    enum class Kind(private val str: String) {
        // Smart casts are completely safe
        STABLE_VALUE("stable"),
        // Smart casts are safe but possible changes in loops / closures ahead must be taken into account
        PREDICTABLE_VARIABLE("predictable"),
        // Smart casts are not safe
        UNPREDICTABLE_VARIABLE("unpredictable"),
        OTHER("other");

        override fun toString() = str

        fun isStable() = this == STABLE_VALUE
    }

    /**
     * Both stable values and predictable local variables are considered "predictable".
     * Predictable means here we do not expect some sudden change of their values,
     * like accessing mutable properties in another thread, so smart casts can be used safely.
     */
    val isPredictable = (kind == Kind.STABLE_VALUE || kind == Kind.PREDICTABLE_VARIABLE)
        @JvmName("isPredictable") get

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataFlowValue) return false

        if (kind.isStable() != other.kind.isStable()) return false
        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }

    override fun toString(): String {
        return kind.toString() + (id?.toString()) + " " + immanentNullability
    }

    override fun hashCode(): Int {
        var result = if (kind.isStable()) 1 else 0
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

    companion object {

        @JvmStatic
        fun nullValue(builtIns: KotlinBuiltIns) = DataFlowValue(
                Object(), builtIns.nullableNothingType, Kind.OTHER, Nullability.NULL
        )

        val ERROR = DataFlowValue(Object(), ErrorUtils.createErrorType("Error type for data flow"), Kind.OTHER, Nullability.IMPOSSIBLE)
    }
}
