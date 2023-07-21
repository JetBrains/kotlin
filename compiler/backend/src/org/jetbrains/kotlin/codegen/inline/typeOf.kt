/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.isSuspendFunctionType
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
    val argument = ReificationArgument(typeParameter.getName().asString(), false, 0)
    ReifiedTypeInliner.putReifiedOperationMarker(ReifiedTypeInliner.OperationKind.TYPE_OF, argument, v)
    v.aconst(null)
    v.areturn(K_TYPE)
    v.visitMaxs(2, 0)
    return node
}

private inline fun InstructionAdapter.unrollArrayIfFewerThan(n: Int, limit: Int, type: Type, element: (Int) -> Unit): Array<Type> {
    if (n < limit) {
        return Array(n) { i ->
            element(i)
            type
        }
    }
    iconst(n)
    newarray(type)
    for (i in 0 until n) {
        dup()
        iconst(i)
        element(i)
        astore(type)
    }
    return arrayOf(AsmUtil.getArrayType(type))
}

fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateTypeOf(
    v: InstructionAdapter, type: KT, intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>
) = generateTypeOf(v, type, intrinsicsSupport, isTypeParameterBound = false)

private fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateTypeOf(
    v: InstructionAdapter, type: KT, intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>, isTypeParameterBound: Boolean
) {
    val typeParameter = type.typeConstructor().getTypeParameterClassifier()
    val methodArguments = if (typeParameter == null) {
        intrinsicsSupport.putClassInstance(v, type)
        val arguments = v.unrollArrayIfFewerThan(type.argumentsCount(), 3, K_TYPE_PROJECTION) { i ->
            generateTypeOfArgument(v, type.getArgument(i), intrinsicsSupport, isTypeParameterBound)
        }
        arrayOf(JAVA_CLASS_TYPE, *arguments)
    } else if (!isTypeParameterBound && typeParameter.isReified()) {
        val argument = ReificationArgument(typeParameter.getName().asString(), type.isMarkedNullable(), 0)
        ReifiedTypeInliner.putReifiedOperationMarker(ReifiedTypeInliner.OperationKind.TYPE_OF, argument, v)
        v.aconst(null)
        return
    } else if (typeReferencesParameterWithRecursiveBound(type)) {
        intrinsicsSupport.reportNonReifiedTypeParameterWithRecursiveBoundUnsupported(typeParameter.getName())
        v.aconst(null)
        return
    } else {
        generateNonReifiedTypeParameter(v, typeParameter, intrinsicsSupport)
        arrayOf(K_CLASSIFIER_TYPE)
    }

    val methodName = if (type.isMarkedNullable()) "nullableTypeOf" else "typeOf"
    val signature = Type.getMethodDescriptor(K_TYPE, *methodArguments)
    v.invokestatic(REFLECTION, methodName, signature, false)

    if (intrinsicsSupport.toKotlinType(type).isSuspendFunctionType) {
        intrinsicsSupport.reportSuspendTypeUnsupported()
    }

    if (intrinsicsSupport.state.config.stableTypeOf) {
        if (intrinsicsSupport.isMutableCollectionType(type)) {
            v.invokestatic(REFLECTION, "mutableCollectionType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
        } else if (type.typeConstructor().isNothingConstructor()) {
            v.invokestatic(REFLECTION, "nothingType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
        }

        if (type.isFlexible()) {
            // If this is a flexible type, we've just generated its lower bound and have it on the stack.
            // Let's generate the upper bound now and call the method that takes lower and upper bound and constructs a flexible KType.
            @Suppress("UNCHECKED_CAST")
            generateTypeOf(v, type.upperBoundIfFlexible() as KT, intrinsicsSupport, isTypeParameterBound)

            v.invokestatic(REFLECTION, "platformType", Type.getMethodDescriptor(K_TYPE, K_TYPE, K_TYPE), false)
        }
    }
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
    v.iconst(if (typeParameter.isReified()) 1 else 0)
    v.invokestatic(
        REFLECTION, "typeParameter",
        Type.getMethodDescriptor(K_TYPE_PARAMETER, OBJECT_TYPE, JAVA_STRING_TYPE, K_VARIANCE, Type.BOOLEAN_TYPE),
        false,
    )

    if (typeParameter.upperBoundCount() == 0) return

    v.dup()
    val argumentsForBounds = v.unrollArrayIfFewerThan(typeParameter.upperBoundCount(), 2, K_TYPE) { i ->
        @Suppress("UNCHECKED_CAST")
        generateTypeOf(v, typeParameter.getUpperBound(i) as KT, intrinsicsSupport, isTypeParameterBound = true)
    }
    v.invokestatic(
        REFLECTION, "setUpperBounds", Type.getMethodDescriptor(Type.VOID_TYPE, K_TYPE_PARAMETER, *argumentsForBounds),
        false
    )
}

private fun TypeSystemCommonBackendContext.typeReferencesParameterWithRecursiveBound(
    type: KotlinTypeMarker,
    used: MutableSet<TypeParameterMarker> = linkedSetOf()
): Boolean {
    val typeParameter = type.typeConstructor().getTypeParameterClassifier()
    if (typeParameter != null) {
        if (!used.add(typeParameter)) return true
        for (i in 0 until typeParameter.upperBoundCount()) {
            if (typeReferencesParameterWithRecursiveBound(typeParameter.getUpperBound(i), used)) return true
        }
        used.remove(typeParameter)
    } else {
        for (i in 0 until type.argumentsCount()) {
            val argument = type.getArgument(i)
            if (!argument.isStarProjection() && typeReferencesParameterWithRecursiveBound(argument.getType(), used)) return true
        }
    }
    return false
}

private fun <KT : KotlinTypeMarker> TypeSystemCommonBackendContext.generateTypeOfArgument(
    v: InstructionAdapter,
    projection: TypeArgumentMarker,
    intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport<KT>,
    isTypeParameterBound: Boolean,
) {
    // KTypeProjection companion members could be made `@JvmStatic`, see KT-30083 and KT-30084
    v.getstatic(K_TYPE_PROJECTION.internalName, "Companion", K_TYPE_PROJECTION_COMPANION.descriptor)
    if (projection.isStarProjection()) {
        v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, "getSTAR", Type.getMethodDescriptor(K_TYPE_PROJECTION), false)
        return
    }

    @Suppress("UNCHECKED_CAST")
    generateTypeOf(v, projection.getType() as KT, intrinsicsSupport, isTypeParameterBound)
    val methodName = when (projection.getVariance()) {
        TypeVariance.INV -> "invariant"
        TypeVariance.IN -> "contravariant"
        TypeVariance.OUT -> "covariant"
    }
    v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, methodName, Type.getMethodDescriptor(K_TYPE_PROJECTION, K_TYPE), false)
}
