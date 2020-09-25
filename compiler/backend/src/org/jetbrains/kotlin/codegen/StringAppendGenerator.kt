/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.google.common.collect.Sets
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class StringAppendGenerator(val useInvokeDynamic: Boolean, val mv: InstructionAdapter) {

    @JvmOverloads
    fun genStringBuilderConstructorIfNeded(swap: Boolean = false) {
        if (useInvokeDynamic) return
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        mv.dup()
        mv.invokespecial("java/lang/StringBuilder", "<init>", "()V", false)
        if (swap) {
            mv.swap()
        }
    }

    fun addArgOnStack(type: Type) {

    }

    fun addCharConstant(value: Int) {
        mv.iconst(value)
        invokeAppend(Type.CHAR_TYPE)
    }

    fun addStringConstant(value: String) {
        mv.aconst(value)
        invokeAppend(JAVA_STRING_TYPE)
    }

    fun invokeAppend(type: Type) {
        if (!useInvokeDynamic) {
            mv.invokevirtual(
                "java/lang/StringBuilder",
                "append",
                "(" + stringBuilderAppendType(type) + ")Ljava/lang/StringBuilder;",
                false
            )
        }
    }

    fun genToString() {
        if (!useInvokeDynamic) {
            mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        }
    }

    companion object {
        private val STRING_BUILDER_OBJECT_APPEND_ARG_TYPES: Set<Type> = Sets.newHashSet(
            AsmTypes.getType(String::class.java),
            AsmTypes.getType(StringBuffer::class.java),
            AsmTypes.getType(CharSequence::class.java)
        )

        private fun stringBuilderAppendType(type: Type): Type {
            return when (type.sort) {
                Type.OBJECT -> if (STRING_BUILDER_OBJECT_APPEND_ARG_TYPES.contains(type)) type else AsmTypes.OBJECT_TYPE
                Type.ARRAY -> AsmTypes.OBJECT_TYPE
                Type.BYTE, Type.SHORT -> Type.INT_TYPE
                else -> type
            }
        }

        fun create(state: GenerationState, mv: InstructionAdapter) = StringAppendGenerator(false, mv)

    }
}