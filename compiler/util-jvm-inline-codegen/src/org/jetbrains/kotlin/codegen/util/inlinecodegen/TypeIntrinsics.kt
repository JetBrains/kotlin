/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

object TypeIntrinsics {
    const val INTRINSICS_CLASS = "kotlin/jvm/internal/TypeIntrinsics"

    const val IS_FUNCTION_OF_ARITY_METHOD_NAME = "isFunctionOfArity"
    const val IS_FUNCTION_OF_ARITY_DESCRIPTOR = "(Ljava/lang/Object;I)Z"

    const val IS_MUTABLE_COLLECTION_METHOD_DESCRIPTOR = "(Ljava/lang/Object;)Z"

    const val BEFORE_CHECKCAST_TO_FUNCTION_OF_ARITY = "beforeCheckcastToFunctionOfArity"
    const val BEFORE_CHECKCAST_TO_FUNCTION_OF_ARITY_DESCRIPTOR = "(Ljava/lang/Object;I)Ljava/lang/Object;"

    /**
     * Returns whether the generation of `is` type check for a given type would require use
     * of intrinsics rather than simple `instanceof`.
     *
     * Shall be in sync with `instanceOf(..)`
     */
    fun isIntrinsicRequiredForInstanceOf(classFqName: String?): Boolean =
        classFqName?.let {
            getFunctionTypeArity(it) >= 0 ||
                    getSuspendFunctionTypeArity(it) >= 0 ||
                    getIsMutableCollectionMethodName(it) != null
        } == true

    fun isCheck(classFqName: String?, nullable: Boolean, boxedAsmTypeInternalName: String, emit: (AbstractInsnNode) -> Unit) {
        if (nullable) {
            val nopeLbl = LabelNode()
            val endLbl = LabelNode()

            emit(InsnNode(Opcodes.DUP))
            emit(JumpInsnNode(Opcodes.IFNULL, nopeLbl))
            instanceOf(classFqName, boxedAsmTypeInternalName, emit)
            emit(JumpInsnNode(Opcodes.GOTO, endLbl))
            emit(nopeLbl)
            emit(InsnNode(Opcodes.POP))
            emit(InsnNode(Opcodes.ICONST_1))
            emit(endLbl)
        } else {
            instanceOf(classFqName, boxedAsmTypeInternalName, emit)
        }
    }

    fun asCast(
        classFqName: String?,
        nullable: Boolean,
        boxedAsmTypeInternalName: String,
        safe: Boolean,
        unifiedNullChecks: Boolean,
        renderedType: String,
        emit: (AbstractInsnNode) -> Unit,
    ) {
        if (!safe) {
            if (!nullable) {
                val nonnullLbl = LabelNode()

                emit(InsnNode(Opcodes.DUP))
                emit(JumpInsnNode(Opcodes.IFNONNULL, nonnullLbl))
                val exceptionClass = if (unifiedNullChecks) "java/lang/NullPointerException" else "kotlin/TypeCastException"
                genThrow(
                    exceptionClass,
                    "null cannot be cast to non-null type $renderedType",
                    emit,
                )
                emit(nonnullLbl)
            }
            checkcast(classFqName, boxedAsmTypeInternalName, emit)
        } else {
            val okLbl = LabelNode()

            emit(InsnNode(Opcodes.DUP))
            instanceOf(classFqName, boxedAsmTypeInternalName, emit)
            emit(JumpInsnNode(Opcodes.IFNE, okLbl))
            emit(InsnNode(Opcodes.POP))
            emit(InsnNode(Opcodes.ACONST_NULL))
            emit(okLbl)
            emit(TypeInsnNode(Opcodes.CHECKCAST, boxedAsmTypeInternalName))
        }
    }

    fun instanceOf(classFqName: String?, boxedAsmTypeInternalName: String, emit: (AbstractInsnNode) -> Unit) {
        val functionTypeArity = classFqName?.let(::getFunctionTypeArity) ?: -1
        if (functionTypeArity >= 0) {
            emit(iconstInsnNode(functionTypeArity))
            emit.typeIntrinsic(IS_FUNCTION_OF_ARITY_METHOD_NAME, IS_FUNCTION_OF_ARITY_DESCRIPTOR)
            return
        }

        val suspendFunctionTypeArity = classFqName?.let(::getSuspendFunctionTypeArity) ?: -1
        if (suspendFunctionTypeArity >= 0) {
            val notSuspendLambdaLbl = LabelNode()
            val endLbl = LabelNode()

            emit(InsnNode(Opcodes.DUP))
            emit(TypeInsnNode(Opcodes.INSTANCEOF, "kotlin/coroutines/jvm/internal/SuspendFunction"))
            emit(JumpInsnNode(Opcodes.IFEQ, notSuspendLambdaLbl))
            emit(iconstInsnNode(suspendFunctionTypeArity + 1))
            emit.typeIntrinsic(IS_FUNCTION_OF_ARITY_METHOD_NAME, IS_FUNCTION_OF_ARITY_DESCRIPTOR)
            emit(JumpInsnNode(Opcodes.GOTO, endLbl))
            emit(notSuspendLambdaLbl)
            emit(InsnNode(Opcodes.POP))
            emit(InsnNode(Opcodes.ICONST_0))
            emit(endLbl)
            return
        }

        val isMutableCollectionMethodName = classFqName?.let(::getIsMutableCollectionMethodName)
        if (isMutableCollectionMethodName != null) {
            emit.typeIntrinsic(isMutableCollectionMethodName, IS_MUTABLE_COLLECTION_METHOD_DESCRIPTOR)
            return
        }

        emit(TypeInsnNode(Opcodes.INSTANCEOF, boxedAsmTypeInternalName))
    }

    fun checkcast(classFqName: String?, boxedAsmTypeInternalName: String, emit: (AbstractInsnNode) -> Unit) {
        val functionTypeArity = classFqName?.let(::getFunctionTypeArity) ?: -1
        if (functionTypeArity >= 0) {
            emit(iconstInsnNode(functionTypeArity))
            emit.typeIntrinsic(BEFORE_CHECKCAST_TO_FUNCTION_OF_ARITY, BEFORE_CHECKCAST_TO_FUNCTION_OF_ARITY_DESCRIPTOR)
            emit(TypeInsnNode(Opcodes.CHECKCAST, boxedAsmTypeInternalName))
            return
        }

        val asMutableCollectionMethodName = classFqName?.let(::getAsMutableCollectionMethodName)
        if (asMutableCollectionMethodName != null) {
            emit.typeIntrinsic(asMutableCollectionMethodName, "(Ljava/lang/Object;)L$boxedAsmTypeInternalName;")
            return
        }

        emit(TypeInsnNode(Opcodes.CHECKCAST, boxedAsmTypeInternalName))
    }
}

private val KOTLIN_FUNCTION_INTERFACE_REGEX = Regex("^kotlin\\.Function([0-9]+)$")
private val KOTLIN_SUSPEND_FUNCTION_INTERFACE_REGEX = Regex("^kotlin\\.coroutines\\.SuspendFunction([0-9]+)$")

private fun getFunctionTypeArityByRegex(classFqName: String, regex: Regex): Int {
    val match = regex.find(classFqName) ?: return -1
    return Integer.valueOf(match.groups[1]!!.value)
}

/**
 * @return function type arity (non-negative), or -1 if the given type is not a function type
 */
fun getFunctionTypeArity(classFqName: String): Int =
    getFunctionTypeArityByRegex(classFqName, KOTLIN_FUNCTION_INTERFACE_REGEX)

/**
 * @return function type arity (non-negative, not counting continuation), or -1 if the given type is not a function type
 */
fun getSuspendFunctionTypeArity(classFqName: String): Int =
    getFunctionTypeArityByRegex(classFqName, KOTLIN_SUSPEND_FUNCTION_INTERFACE_REGEX)

private fun getMutableCollectionMethodName(prefix: String, fqName: String): String? {
    val baseName = when (fqName) {
        "kotlin.collections.MutableIterator" -> "MutableIterator"
        "kotlin.collections.MutableIterable" -> "MutableIterable"
        "kotlin.collections.MutableCollection" -> "MutableCollection"
        "kotlin.collections.MutableList" -> "MutableList"
        "kotlin.collections.MutableListIterator" -> "MutableListIterator"
        "kotlin.collections.MutableSet" -> "MutableSet"
        "kotlin.collections.MutableMap" -> "MutableMap"
        "kotlin.collections.MutableMap.MutableEntry" -> "MutableMapEntry"
        else -> return null
    }
    return prefix + baseName
}

fun getIsMutableCollectionMethodName(fqName: String): String? =
    getMutableCollectionMethodName("is", fqName)

fun getAsMutableCollectionMethodName(fqName: String): String? =
    getMutableCollectionMethodName("as", fqName)

private fun genThrow(exception: String, message: String?, emit: (AbstractInsnNode) -> Unit) {
    emit(TypeInsnNode(Opcodes.NEW, exception))
    emit(InsnNode(Opcodes.DUP))
    if (message != null) {
        emit(LdcInsnNode(message))
        emit(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                exception,
                "<init>",
                "(Ljava/lang/String;)V",
                false,
            )
        )
    } else {
        emit(
            MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                exception,
                "<init>",
                "()V",
                false,
            )
        )
    }
    emit(InsnNode(Opcodes.ATHROW))
}

private fun ((AbstractInsnNode) -> Unit).typeIntrinsic(methodName: String, descriptor: String) =
    this(MethodInsnNode(Opcodes.INVOKESTATIC, TypeIntrinsics.INTRINSICS_CLASS, methodName, descriptor, false))
