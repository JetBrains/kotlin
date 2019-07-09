/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

class DefaultCallArgs(val size: Int) {

    private val bits: BitSet = BitSet(size)

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
        for (i in 0 until size) {
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
                callGenerator.putValueIfNeeded(
                    JvmKotlinType(Type.INT_TYPE), StackValue.constant(mask, Type.INT_TYPE), ValueKind.DEFAULT_MASK
                )
            }

            val parameterType = if (isConstructor) AsmTypes.DEFAULT_CONSTRUCTOR_MARKER else AsmTypes.OBJECT_TYPE
            callGenerator.putValueIfNeeded(
                JvmKotlinType(parameterType), StackValue.constant(null, parameterType), ValueKind.METHOD_HANDLE_IN_DEFAULT
            )
        }
        return toInts.isNotEmpty()
    }
}