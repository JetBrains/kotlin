/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode

class FixStackInterpreter : BasicTypeInterpreter<FixStackValue>() {
    override fun uninitializedValue() = FixStackValue.UNINITIALIZED
    override fun booleanValue() = FixStackValue.INT
    override fun charValue() = FixStackValue.INT
    override fun byteValue() = FixStackValue.INT
    override fun shortValue() = FixStackValue.INT
    override fun intValue() = FixStackValue.INT
    override fun longValue() = FixStackValue.LONG
    override fun floatValue() = FixStackValue.FLOAT
    override fun doubleValue() = FixStackValue.DOUBLE
    override fun nullValue() = FixStackValue.OBJECT
    override fun objectValue(type: Type) = FixStackValue.OBJECT
    override fun arrayValue(type: Type) = FixStackValue.OBJECT
    override fun methodValue(type: Type) = FixStackValue.OBJECT
    override fun handleValue(handle: Handle) = FixStackValue.OBJECT
    override fun typeConstValue(typeConst: Type) = FixStackValue.OBJECT
    override fun aaLoadValue(arrayValue: FixStackValue) = FixStackValue.OBJECT

    override fun copyOperation(insn: AbstractInsnNode, value: FixStackValue): FixStackValue =
        when (insn.opcode) {
            ILOAD -> FixStackValue.INT
            LLOAD -> FixStackValue.LONG
            FLOAD -> FixStackValue.FLOAT
            DLOAD -> FixStackValue.DOUBLE
            ALOAD -> FixStackValue.OBJECT
            else -> value
        }

    override fun merge(v: FixStackValue, w: FixStackValue): FixStackValue =
        if (v == w)
            v
        else
            throw AssertionError("Mismatching value kinds: $v != $w")
}
