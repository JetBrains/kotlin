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
import kotlin.reflect.KVariance

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

internal fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateTypeOf(
    v: InstructionAdapter, type: KT, intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
) {
    val typeParameter = type.typeConstructor().getTypeParameterClassifier()
    if (typeParameter != null) {
        if (!doesTypeContainTypeParametersWithRecursiveBounds(type)) {
            throw UnsupportedOperationException(
                "Non-reified type parameters with recursive bounds are not supported yet: ${typeParameter.getName()}"
            )
        }

        generateNonReifiedTypeParameter(v, typeParameter, intrinsicsSupport)
    } else {
        intrinsicsSupport.putClassInstance(v, type)
    }

    val argumentsSize = type.argumentsCount()
    val useArray = argumentsSize >= 3

    if (useArray) {
        v.iconst(argumentsSize)
        v.newarray(K_TYPE_PROJECTION)
    }

    for (i in 0 until argumentsSize) {
        if (useArray) {
            v.dup()
            v.iconst(i)
        }

        doGenerateTypeProjection(v, type.getArgument(i), intrinsicsSupport)

        if (useArray) {
            v.astore(K_TYPE_PROJECTION)
        }
    }

    val methodName = if (type.isMarkedNullable()) "nullableTypeOf" else "typeOf"

    val signature = if (typeParameter != null) {
        Type.getMethodDescriptor(K_TYPE, K_CLASSIFIER_TYPE)
    } else {
        val projections = when (argumentsSize) {
            0 -> emptyArray()
            1 -> arrayOf(K_TYPE_PROJECTION)
            2 -> arrayOf(K_TYPE_PROJECTION, K_TYPE_PROJECTION)
            else -> arrayOf(AsmUtil.getArrayType(K_TYPE_PROJECTION))
        }
        Type.getMethodDescriptor(K_TYPE, JAVA_CLASS_TYPE, *projections)
    }

    v.invokestatic(REFLECTION, methodName, signature, false)
}

private fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateNonReifiedTypeParameter(
    v: InstructionAdapter, typeParameter: TypeParameterMarker, intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
) {
    intrinsicsSupport.generateTypeParameterContainer(v, typeParameter)

    v.aconst(typeParameter.getName().asString())
    val variance = when (typeParameter.getVariance()) {
        TypeVariance.INV -> KVariance.INVARIANT
        TypeVariance.IN -> KVariance.IN
        TypeVariance.OUT -> KVariance.OUT
    }
    v.getstatic(K_VARIANCE.internalName, variance.name, K_VARIANCE.descriptor)
    v.aconst(typeParameter.isReified())
    v.invokestatic(
        REFLECTION, "typeParameter",
        Type.getMethodDescriptor(K_TYPE_PARAMETER, OBJECT_TYPE, JAVA_STRING_TYPE, K_VARIANCE, Type.BOOLEAN_TYPE),
        false,
    )

    @Suppress("UNCHECKED_CAST")
    val bounds = (0 until typeParameter.upperBoundCount()).map { typeParameter.getUpperBound(it) as KT }
    if (bounds.isEmpty()) return

    v.dup()

    if (bounds.size == 1) {
        generateTypeOf(v, bounds.single(), intrinsicsSupport)
    } else {
        v.aconst(bounds.size)
        v.newarray(K_TYPE)
        for ((i, bound) in bounds.withIndex()) {
            v.dup()
            v.aconst(i)
            generateTypeOf(v, bound, intrinsicsSupport)
            v.astore(K_TYPE)
        }
    }

    v.invokestatic(
        REFLECTION, "setUpperBounds", Type.getMethodDescriptor(
            Type.VOID_TYPE, K_TYPE_PARAMETER,
            if (bounds.size == 1) K_TYPE else AsmUtil.getArrayType(K_TYPE)
        ),
        false
    )
}

private fun TypeSystemCommonBackendContext.doesTypeContainTypeParametersWithRecursiveBounds(
    type: KotlinTypeMarker,
    used: MutableSet<TypeParameterMarker> = linkedSetOf()
): Boolean {
    val typeParameter = type.typeConstructor().getTypeParameterClassifier()
    if (typeParameter != null) {
        if (!used.add(typeParameter)) return false
        for (i in 0 until typeParameter.upperBoundCount()) {
            if (!doesTypeContainTypeParametersWithRecursiveBounds(typeParameter.getUpperBound(i), used)) return false
        }
        used.remove(typeParameter)
    } else {
        for (i in 0 until type.argumentsCount()) {
            val argument = type.getArgument(i)
            if (!argument.isStarProjection() && !doesTypeContainTypeParametersWithRecursiveBounds(argument.getType(), used)) return false
        }
    }
    return true
}

private fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.doGenerateTypeProjection(
    v: InstructionAdapter,
    projection: TypeArgumentMarker,
    intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
) {
    // KTypeProjection members could be static, see KT-30083 and KT-30084
    v.getstatic(K_TYPE_PROJECTION.internalName, "Companion", K_TYPE_PROJECTION_COMPANION.descriptor)

    if (projection.isStarProjection()) {
        v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, "getSTAR", Type.getMethodDescriptor(K_TYPE_PROJECTION), false)
        return
    }

    @Suppress("UNCHECKED_CAST")
    val type = projection.getType() as KT
    val typeParameterClassifier = type.typeConstructor().getTypeParameterClassifier()
    if (typeParameterClassifier != null && typeParameterClassifier.isReified()) {
        putTypeOfReifiedTypeParameter(v, typeParameterClassifier, type.isMarkedNullable())
    } else {
        generateTypeOf(v, type, intrinsicsSupport)
    }

    val methodName = when (projection.getVariance()) {
        TypeVariance.INV -> "invariant"
        TypeVariance.IN -> "contravariant"
        TypeVariance.OUT -> "covariant"
    }
    v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, methodName, Type.getMethodDescriptor(K_TYPE_PROJECTION, K_TYPE), false)
}
