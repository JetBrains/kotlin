/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.google.common.collect.Sets
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmRuntimeStringConcat
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.lang.StringBuilder

class StringConcatGenerator(val mode: JvmRuntimeStringConcat, val mv: InstructionAdapter) {

    private val template = StringBuilder("")
    private val paramTypes = arrayListOf<Type>()

    @JvmOverloads
    fun genStringBuilderConstructorIfNeded(swap: Boolean = false) {
        if (mode.isDynamic) return
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        mv.dup()
        mv.invokespecial("java/lang/StringBuilder", "<init>", "()V", false)
        if (swap) {
            mv.swap()
        }
    }

    fun addCharConstant(value: Char) {
        if (!mode.isDynamic) {
            mv.iconst(value.toInt())
            invokeAppend(Type.CHAR_TYPE)
        } else {
            template.append(value)
        }
    }

    fun addStringConstant(value: String) {
        if (!mode.isDynamic) {
            mv.aconst(value)
            invokeAppend(JAVA_STRING_TYPE)
        } else {
            template.append(value)
        }
    }

    fun invokeAppend(type: Type) {
        if (!mode.isDynamic) {
            mv.invokevirtual(
                "java/lang/StringBuilder",
                "append",
                "(" + stringBuilderAppendType(type) + ")Ljava/lang/StringBuilder;",
                false
            )
        } else {
            paramTypes.add(type)
            template.append("\u0001")
            if (paramTypes.size == 200) {
                // Concatenate current arguments into string
                // because of `StringConcatFactory` limitation add use it as new argument for further processing:
                // "The number of parameter slots in {@code concatType} is less than or equal to 200"
                genToString()
            }
        }
    }

    fun genToString() {
        if (!mode.isDynamic) {
            mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        } else {
            //if state was flushed in `invokeAppend` do nothing
            if (template.isEmpty() && paramTypes.size == 1 && paramTypes[0] == JAVA_STRING_TYPE) return
            val bootstrap = Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/StringConcatFactory",
                "makeConcatWithConstants",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                false
            )

            mv.invokedynamic(
                "makeConcatWithConstants",
                Type.getMethodDescriptor(JAVA_STRING_TYPE, *paramTypes.toTypedArray()),
                bootstrap,
                arrayOf(template.toString())
            )
            template.clear()
            paramTypes.clear()
            paramTypes.add(JAVA_STRING_TYPE)
            template.append("\u0001")
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

        fun create(state: GenerationState, mv: InstructionAdapter) =
            StringConcatGenerator(state.runtimeStringConcat, mv)

    }
}