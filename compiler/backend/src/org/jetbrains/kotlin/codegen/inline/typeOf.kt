/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal fun TypeSystemCommonBackendContext.createTypeOfMethodBody(typeParameter: TypeParameterMarker): MethodNode {
    val node = MethodNode(Opcodes.API_VERSION, Opcodes.ACC_STATIC, "fake", Type.getMethodDescriptor(K_TYPE), null, null)
    val v = InstructionAdapter(node)

    putTypeOfReifiedTypeParameter(v, typeParameter, false)
    v.areturn(K_TYPE)

    v.visitMaxs(2, 0)

    return node
}

private fun TypeSystemCommonBackendContext.putTypeOfReifiedTypeParameter(
    v: InstructionAdapter, typeParameter: TypeParameterMarker, isNullable: Boolean
) {
    ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(typeParameter, isNullable, ReifiedTypeInliner.OperationKind.TYPE_OF, v, this)
    v.aconst(null)
}

// Returns some upper bound on maximum stack size
internal fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateTypeOf(
    v: InstructionAdapter, type: KT, intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
): Int {
    intrinsicsSupport.putClassInstance(v, type)

    val argumentsSize = type.argumentsCount()
    val useArray = argumentsSize >= 3

    if (useArray) {
        v.iconst(argumentsSize)
        v.newarray(K_TYPE_PROJECTION)
    }

    var maxStackSize = 3

    for (i in 0 until argumentsSize) {
        if (useArray) {
            v.dup()
            v.iconst(i)
        }

        val stackSize = doGenerateTypeProjection(v, type.getArgument(i), intrinsicsSupport)
        maxStackSize = maxOf(maxStackSize, stackSize + i + 5)

        if (useArray) {
            v.astore(K_TYPE_PROJECTION)
        }
    }

    val methodName = if (type.isMarkedNullable()) "nullableTypeOf" else "typeOf"

    val projections = when (argumentsSize) {
        0 -> emptyArray()
        1 -> arrayOf(K_TYPE_PROJECTION)
        2 -> arrayOf(K_TYPE_PROJECTION, K_TYPE_PROJECTION)
        else -> arrayOf(AsmUtil.getArrayType(K_TYPE_PROJECTION))
    }
    val signature = Type.getMethodDescriptor(K_TYPE, JAVA_CLASS_TYPE, *projections)

    v.invokestatic(REFLECTION, methodName, signature, false)

    return maxStackSize
}

private fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.doGenerateTypeProjection(
    v: InstructionAdapter,
    projection: TypeArgumentMarker,
    intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
): Int {
    // KTypeProjection members could be static, see KT-30083 and KT-30084
    v.getstatic(K_TYPE_PROJECTION.internalName, "Companion", K_TYPE_PROJECTION_COMPANION.descriptor)

    if (projection.isStarProjection()) {
        v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, "getSTAR", Type.getMethodDescriptor(K_TYPE_PROJECTION), false)
        return 1
    }

    @Suppress("UNCHECKED_CAST")
    val type = projection.getType() as KT
    val typeParameterClassifier = type.typeConstructor().getTypeParameterClassifier()
    val stackSize = if (typeParameterClassifier != null) {
        if (typeParameterClassifier.isReified()) {
            putTypeOfReifiedTypeParameter(v, typeParameterClassifier, type.isMarkedNullable())
            2
        } else {
            // TODO: support non-reified type parameters in typeOf
            @Suppress("UNCHECKED_CAST")
            generateTypeOf(v, nullableAnyType() as KT, intrinsicsSupport)
        }
    } else {
        generateTypeOf(v, type, intrinsicsSupport)
    }

    val methodName = when (projection.getVariance()) {
        TypeVariance.INV -> "invariant"
        TypeVariance.IN -> "contravariant"
        TypeVariance.OUT -> "covariant"
    }
    v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, methodName, Type.getMethodDescriptor(K_TYPE_PROJECTION, K_TYPE), false)

    return stackSize + 1
}
