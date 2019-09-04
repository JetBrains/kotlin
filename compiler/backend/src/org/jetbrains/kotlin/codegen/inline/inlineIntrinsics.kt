/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.common.isBuiltInIntercepted
import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForIntercepted
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.createMethodNodeForAlwaysEnabledAssert
import org.jetbrains.kotlin.codegen.isBuiltinAlwaysEnabledAssert
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.TypeOfChecker
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
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

internal fun generateInlineIntrinsic(
    state: GenerationState,
    descriptor: FunctionDescriptor,
    typeParameters: List<TypeParameterMarker>?,
    typeSystem: TypeSystemCommonBackendContext
): MethodNode? {
    val languageVersionSettings = state.languageVersionSettings

    return when {
        isSpecialEnumMethod(descriptor) ->
            createSpecialEnumMethodBody(descriptor.name.asString(), typeParameters!!.single(), typeSystem)
        TypeOfChecker.isTypeOf(descriptor) ->
            typeSystem.createTypeOfMethodBody(typeParameters!!.single())
        descriptor.isBuiltInIntercepted(languageVersionSettings) ->
            createMethodNodeForIntercepted(languageVersionSettings)
        descriptor.isBuiltInCoroutineContext(languageVersionSettings) ->
            createMethodNodeForCoroutineContext(descriptor, languageVersionSettings)
        descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn(languageVersionSettings) ->
            createMethodNodeForSuspendCoroutineUninterceptedOrReturn(languageVersionSettings)
        descriptor.isBuiltinAlwaysEnabledAssert() ->
            createMethodNodeForAlwaysEnabledAssert(descriptor)
        else -> null
    }
}

private fun isSpecialEnumMethod(descriptor: FunctionDescriptor): Boolean {
    val containingDeclaration = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
    if (containingDeclaration.fqName != KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) {
        return false
    }
    if (descriptor.typeParameters.size != 1) {
        return false
    }
    val name = descriptor.name.asString()
    val parameters = descriptor.valueParameters
    return name == "enumValues" && parameters.size == 0 ||
            (name == "enumValueOf" && parameters.size == 1 && KotlinBuiltIns.isString(parameters[0].type))
}

private fun createSpecialEnumMethodBody(
    name: String, typeParameter: TypeParameterMarker, typeSystem: TypeSystemCommonBackendContext
): MethodNode {
    val isValueOf = "enumValueOf" == name
    val desc = getSpecialEnumFunDescriptor(ENUM_TYPE, isValueOf)
    val node = MethodNode(Opcodes.API_VERSION, Opcodes.ACC_STATIC, "fake", desc, null, null)
    ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
        typeParameter, false, ReifiedTypeInliner.OperationKind.ENUM_REIFIED, InstructionAdapter(node), typeSystem
    )
    if (isValueOf) {
        node.visitInsn(Opcodes.ACONST_NULL)
        node.visitVarInsn(Opcodes.ALOAD, 0)

        node.visitMethodInsn(
            Opcodes.INVOKESTATIC, ENUM_TYPE.internalName, "valueOf",
            Type.getMethodDescriptor(ENUM_TYPE, JAVA_CLASS_TYPE, JAVA_STRING_TYPE), false
        )
    } else {
        node.visitInsn(Opcodes.ICONST_0)
        node.visitTypeInsn(Opcodes.ANEWARRAY, ENUM_TYPE.internalName)
    }
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(if (isValueOf) 3 else 2, if (isValueOf) 1 else 0)
    return node
}

internal fun getSpecialEnumFunDescriptor(type: Type, isValueOf: Boolean): String =
    if (isValueOf) Type.getMethodDescriptor(type, JAVA_STRING_TYPE)
    else Type.getMethodDescriptor(AsmUtil.getArrayType(type))

private fun TypeSystemCommonBackendContext.createTypeOfMethodBody(typeParameter: TypeParameterMarker): MethodNode {
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
