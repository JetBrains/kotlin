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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * Low level abstraction for bounded range that is used to generate contains checks and for loops.
 */
class BoundedValue(
    val lowBound: StackValue,
    val isLowInclusive: Boolean = true,
    val highBound: StackValue,
    val isHighInclusive: Boolean = true
) {

    // It is necessary to maintain the proper evaluation order as of Kotlin 1.0 and 1.1
    // to evaluate range bounds left to right and put them on stack as 'high; low'.
    fun putHighLow(v: InstructionAdapter, type: Type) {
        if (!lowBound.canHaveSideEffects() || !highBound.canHaveSideEffects()) {
            highBound.put(type, v)
            lowBound.put(type, v)
        } else {
            lowBound.put(type, v)
            highBound.put(type, v)
            AsmUtil.swap(v, type, type)
        }
    }
}