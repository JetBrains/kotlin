/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.google.common.collect.Sets
import org.jetbrains.kotlin.codegen.BranchedValue.Companion.FALSE
import org.jetbrains.kotlin.codegen.BranchedValue.Companion.TRUE
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmRuntimeStringConcat
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.lang.StringBuilder

class StringConcatGenerator(val mode: JvmRuntimeStringConcat, val mv: InstructionAdapter) {

    private val template = StringBuilder("")
    private val paramTypes = arrayListOf<Type>()
    private var justFlushed = false

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

    @JvmOverloads
    fun putValueOrProcessConstant(stackValue: StackValue, type: Type = stackValue.type, kotlinType: KotlinType? = stackValue.kotlinType) {
        justFlushed = false
        if (mode == JvmRuntimeStringConcat.ENABLE) {
            when (stackValue) {
                is StackValue.Constant -> {
                    template.append(stackValue.value)
                    return
                }
                TRUE -> {
                    template.append(true)
                    return
                }
                FALSE -> {
                    template.append(false)
                    return
                }
            }
        }
        stackValue.put(type, kotlinType, mv)
        invokeAppend(type)
    }

    fun addStringConstant(value: String) {
        putValueOrProcessConstant(StackValue.constant(value, JAVA_STRING_TYPE, null))
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
            justFlushed = false
            paramTypes.add(type)
            template.append("\u0001")
            if (paramTypes.size == 200) {
                // Concatenate current arguments into string
                // because of `StringConcatFactory` limitation add use it as new argument for further processing:
                // "The number of parameter slots in {@code concatType} is less than or equal to 200"
                genToString()
                justFlushed = true
            }
        }
    }

    fun genToString() {
        if (!mode.isDynamic) {
            mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        } else {
            //if state was flushed in `invokeAppend` do nothing
            if (justFlushed) return
            if (mode == JvmRuntimeStringConcat.ENABLE) {
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
            } else {
                val bootstrap = Handle(
                    Opcodes.H_INVOKESTATIC,
                    "java/lang/invoke/StringConcatFactory",
                    "makeConcat",
                    "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                    false
                )

                mv.invokedynamic(
                    "makeConcat",
                    Type.getMethodDescriptor(JAVA_STRING_TYPE, *paramTypes.toTypedArray()),
                    bootstrap,
                    arrayOf()
                )
            }
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