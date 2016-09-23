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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

class DefaultCallArgs(val size: Int) {

    val bits: BitSet = BitSet(size)

    fun mark(index: Int) {
        assert (index < size) {
            "Mask index should be less then size, but $index >= $size"
        }
        bits.set(index)
    }

    fun toInts(): List<Int> {
        if (bits.isEmpty || size == 0) {
            return emptyList()
        }

        val masks = ArrayList<Int>(1)

        var mask = 0
        for (i in 0..size - 1) {
            if (i != 0 && i % Integer.SIZE == 0) {
                masks.add(mask)
                mask = 0
            }
            mask = mask or if (bits.get(i)) 1 shl (i % Integer.SIZE) else 0
        }
        masks.add(mask)

        return masks
    }

    fun generateOnStackIfNeeded(callGenerator: CallGenerator, isConstructor: Boolean): Boolean {
        val toInts = toInts()
        if (!toInts.isEmpty()) {
            for (mask in toInts) {
                callGenerator.putValueIfNeeded(Type.INT_TYPE, StackValue.constant(mask, Type.INT_TYPE))
            }

            val parameterType = if (isConstructor) AsmTypes.DEFAULT_CONSTRUCTOR_MARKER else AsmTypes.OBJECT_TYPE
            callGenerator.putValueIfNeeded(parameterType, StackValue.constant(null, parameterType))
        }
        return toInts.isNotEmpty()
    }

    fun generateOnStackIfNeeded(iv: InstructionAdapter, isConstructor: Boolean): Boolean {
        val toInts = toInts()
        if (!toInts.isEmpty()) {
            for (mask in toInts) {
                iv.iconst(mask)
            }

            val parameterType = if (isConstructor) AsmTypes.DEFAULT_CONSTRUCTOR_MARKER else AsmTypes.OBJECT_TYPE
            iv.aconst(null)
        }
        return toInts.isNotEmpty()
    }
}