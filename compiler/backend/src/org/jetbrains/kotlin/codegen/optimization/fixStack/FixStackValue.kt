/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.analysis.Value

enum class FixStackValue(
    private val _size: Int,
    val loadOpcode: Int,
    val storeOpcode: Int,
    val popOpcode: Int
) : Value {
    INT(1, Opcodes.ILOAD, Opcodes.ISTORE, Opcodes.POP),
    LONG(2, Opcodes.LLOAD, Opcodes.LSTORE, Opcodes.POP2),
    FLOAT(1, Opcodes.FLOAD, Opcodes.FSTORE, Opcodes.POP),
    DOUBLE(2, Opcodes.DLOAD, Opcodes.DSTORE, Opcodes.POP2),
    OBJECT(1, Opcodes.ALOAD, Opcodes.ASTORE, Opcodes.POP)
    ;

    override fun getSize(): Int = _size
}

fun Type.toFixStackValue(): FixStackValue? =
    when (this.sort) {
        Type.VOID -> null
        Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> FixStackValue.INT
        Type.LONG -> FixStackValue.LONG
        Type.FLOAT -> FixStackValue.FLOAT
        Type.DOUBLE -> FixStackValue.DOUBLE
        Type.OBJECT, Type.ARRAY, Type.METHOD -> FixStackValue.OBJECT
        else -> throw AssertionError("Unexpected type: $this")
    }