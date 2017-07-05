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

package org.jetbrains.kotlin.codegen.range.comparison

import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface ComparisonGenerator {
    val comparedType: Type

    fun jumpIfGreaterOrEqual(v: InstructionAdapter, label: Label)
    fun jumpIfLessOrEqual(v: InstructionAdapter, label: Label)
    fun jumpIfGreater(v: InstructionAdapter, label: Label)
    fun jumpIfLess(v: InstructionAdapter, label: Label)
}

fun getComparisonGeneratorForPrimitiveType(type: Type): ComparisonGenerator =
        when (type) {
            Type.INT_TYPE, Type.SHORT_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE -> IntComparisonGenerator
            Type.LONG_TYPE -> LongComparisonGenerator
            Type.FLOAT_TYPE -> FloatComparisonGenerator
            Type.DOUBLE_TYPE -> DoubleComparisonGenerator
            else -> throw UnsupportedOperationException("Unexpected primitive type: " + type)
        }