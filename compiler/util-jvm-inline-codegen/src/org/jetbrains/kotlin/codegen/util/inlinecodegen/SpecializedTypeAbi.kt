/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.InsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode

sealed interface SpecializedTypeAbi {

    val loadStoreReturnOpcodeOffset: Int
    val defaultOpcode: Int
    val reprDesc: String
    val boxedInternalName: String
    val isWide: Boolean get() = reprDesc == "J" || reprDesc == "D"

    fun genBox(instructions: InsnList, targetInsn: AbstractInsnNode)
    fun genUnbox(instructions: InsnList, targetInsn: AbstractInsnNode)
    fun genCoerce2Nullable(instructions: InsnList, targetInsn: AbstractInsnNode)
    fun genCoerce2NonNullable(instructions: InsnList, targetInsn: AbstractInsnNode)

    companion object {
        fun fromLightIrType(type: LightIrType): SpecializedTypeAbi? {
            val classifier = type.classifier as? LightIrType.Classifier.Clazz ?: return null

            // Simple non-null primitive casse
            if (!type.nullable) ktPrimitiveToSpecializedType(classifier.fqName)?.let { return it }

            // Inline value class
            if (classifier.inlineAbi != null) {
                return InlineClass(
                    classifier.fqName.replace('.', '/'),
                    type.nullable,
                    classifier.inlineAbi,
                )
            }

            return null
        }
    }
}

private fun ktPrimitiveToSpecializedType(fqName: String) = when (fqName) {
    "kotlin.Boolean" -> Primitive("Z", "boolean", "java/lang/Boolean", 0, Opcodes.ICONST_0)
    "kotlin.Char" -> Primitive("C", "char", "java/lang/Character", 0, Opcodes.ICONST_0)
    "kotlin.Byte" -> Primitive("B", "byte", "java/lang/Byte", 0, Opcodes.ICONST_0)
    "kotlin.Short" -> Primitive("S", "short", "java/lang/Short", 0, Opcodes.ICONST_0)
    "kotlin.Int" -> Primitive("I", "int", "java/lang/Integer", 0, Opcodes.ICONST_0)
    "kotlin.Float" -> Primitive("F", "float", "java/lang/Float", 2, Opcodes.FCONST_0)
    "kotlin.Long" -> Primitive("J", "long", "java/lang/Long", 1, Opcodes.LCONST_0)
    "kotlin.Double" -> Primitive("D", "double", "java/lang/Double", 3, Opcodes.DCONST_0)
    else -> null
}

data class Primitive(
    override val reprDesc: String,
    val javaName: String,
    override val boxedInternalName: String,
    override val loadStoreReturnOpcodeOffset: Int,
    override val defaultOpcode: Int,
) : SpecializedTypeAbi {

    override fun genBox(instructions: InsnList, targetInsn: AbstractInsnNode) {
        instructions.set(
            targetInsn,
            MethodInsnNode(
                Opcodes.INVOKESTATIC,
                boxedInternalName,
                "valueOf",
                "($reprDesc)L$boxedInternalName;",
                false,
            )
        )
    }

    override fun genUnbox(instructions: InsnList, targetInsn: AbstractInsnNode) {
        instructions.insertBefore(targetInsn, TypeInsnNode(Opcodes.CHECKCAST, boxedInternalName))
        instructions.set(
            targetInsn,
            MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                boxedInternalName,
                "${javaName}Value",
                "()$reprDesc",
                false
            )
        )
    }

    override fun genCoerce2Nullable(instructions: InsnList, targetInsn: AbstractInsnNode) {
        genBox(instructions, targetInsn)
    }

    override fun genCoerce2NonNullable(instructions: InsnList, targetInsn: AbstractInsnNode) {
        genUnbox(instructions, targetInsn)
    }

    companion object {
        fun fromDesc(desc: String): Primitive? {
            return when (desc) {
                "Z" -> Primitive("Z", "boolean", "java/lang/Boolean", 0, Opcodes.ICONST_0)
                "C" -> Primitive("C", "char", "java/lang/Character", 0, Opcodes.ICONST_0)
                "B" -> Primitive("B", "byte", "java/lang/Byte", 0, Opcodes.ICONST_0)
                "S" -> Primitive("S", "short", "java/lang/Short", 0, Opcodes.ICONST_0)
                "I" -> Primitive("I", "int", "java/lang/Integer", 0, Opcodes.ICONST_0)
                "F" -> Primitive("F", "float", "java/lang/Float", 2, Opcodes.FCONST_0)
                "J" -> Primitive("J", "long", "java/lang/Long", 1, Opcodes.LCONST_0)
                "D" -> Primitive("D", "double", "java/lang/Double", 3, Opcodes.DCONST_0)
                else -> null
            }
        }
    }
}

data class InlineClass(
    override val boxedInternalName: String,
    val isNullable: Boolean,
    val inlineAbi: LightIrType.InlineAbi,
) : SpecializedTypeAbi {
    val asPrimitive = Primitive.fromDesc(reprDesc)
    override val loadStoreReturnOpcodeOffset get() = asPrimitive?.loadStoreReturnOpcodeOffset ?: 4
    override val defaultOpcode get() = asPrimitive?.defaultOpcode ?: Opcodes.ACONST_NULL
    val boxedDesc: String get() = "L$boxedInternalName;"
    override val reprDesc get() = if (isNullable && inlineAbi.nullableIsBoxed) boxedDesc else inlineAbi.unboxedDesc

    override fun genBox(instructions: InsnList, targetInsn: AbstractInsnNode) {
        if (reprDesc != boxedDesc) {
            instructions.set(
                targetInsn,
                MethodInsnNode(Opcodes.INVOKESTATIC, boxedInternalName, "box-impl", "(${reprDesc})L$boxedInternalName;", false)
            )
        } else {
            instructions.set(targetInsn, InsnNode(Opcodes.NOP))
        }
    }

    override fun genUnbox(instructions: InsnList, targetInsn: AbstractInsnNode) {
        if (reprDesc != boxedDesc) {
            instructions.insertBefore(targetInsn, TypeInsnNode(Opcodes.CHECKCAST, boxedInternalName))
            instructions.set(
                targetInsn,
                MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    boxedInternalName,
                    "unbox-impl",
                    "()${reprDesc}",
                    false
                )
            )
        } else {
            instructions.set(targetInsn, InsnNode(Opcodes.NOP))
        }
    }

    override fun genCoerce2Nullable(instructions: InsnList, targetInsn: AbstractInsnNode) {
        if (inlineAbi.nullableIsBoxed) {
            genBox(instructions, targetInsn)
        } else {
            instructions.set(targetInsn, InsnNode(Opcodes.NOP))
        }
    }

    override fun genCoerce2NonNullable(instructions: InsnList, targetInsn: AbstractInsnNode) {
        if (inlineAbi.nullableIsBoxed) {
            genUnbox(instructions, targetInsn)
        } else {
            instructions.set(targetInsn, InsnNode(Opcodes.NOP))
        }
    }
}
