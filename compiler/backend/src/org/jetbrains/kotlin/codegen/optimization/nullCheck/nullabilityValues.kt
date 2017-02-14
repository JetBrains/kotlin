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

package org.jetbrains.kotlin.codegen.optimization.nullCheck

import org.jetbrains.kotlin.codegen.optimization.boxing.ProgressionIteratorBasicValue
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

class NotNullBasicValue(type: Type?) : StrictBasicValue(type) {
    override fun equals(other: Any?): Boolean = other is NotNullBasicValue
    // We do not differ not-nullable values, so we should always return the same hashCode
    // Actually it doesn't really matter because analyzer is not supposed to store values in hashtables
    override fun hashCode() = 0

    companion object {
        val NOT_NULL_REFERENCE_VALUE = NotNullBasicValue(StrictBasicValue.REFERENCE_VALUE.type)
    }
}

object NullBasicValue : StrictBasicValue(AsmTypes.OBJECT_TYPE)

enum class Nullability {
    NULL, NOT_NULL, NULLABLE;
    fun isNull() = this == NULL
    fun isNotNull() = this == NOT_NULL
}

fun BasicValue.getNullability(): Nullability =
        when (this) {
            is NullBasicValue -> Nullability.NULL
            is NotNullBasicValue -> Nullability.NOT_NULL
            is ProgressionIteratorBasicValue -> Nullability.NOT_NULL
            else -> Nullability.NULLABLE
        }