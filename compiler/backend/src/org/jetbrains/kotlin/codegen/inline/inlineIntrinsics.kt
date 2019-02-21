/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.common.isBuiltInIntercepted
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForIntercepted
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.coroutines.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm
import org.jetbrains.kotlin.codegen.createMethodNodeForAlwaysEnabledAssert
import org.jetbrains.kotlin.codegen.isBuiltinAlwaysEnabledAssert
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.TypeOfChecker
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal fun generateInlineIntrinsic(
    state: GenerationState,
    descriptor: FunctionDescriptor,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?
): MethodNode? {
    val languageVersionSettings = state.languageVersionSettings
    val typeMapper = state.typeMapper
    return when {
        isSpecialEnumMethod(descriptor) ->
            createSpecialEnumMethodBody(descriptor.name.asString(), typeArguments!!.keys.single().defaultType, typeMapper)
        TypeOfChecker.isTypeOf(descriptor) ->
            createTypeOfMethodBody(typeArguments!!.keys.single().defaultType)
        descriptor.isBuiltInIntercepted(languageVersionSettings) ->
            createMethodNodeForIntercepted(descriptor, typeMapper, languageVersionSettings)
        descriptor.isBuiltInCoroutineContext(languageVersionSettings) ->
            createMethodNodeForCoroutineContext(descriptor, languageVersionSettings)
        descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm(languageVersionSettings) ->
            createMethodNodeForSuspendCoroutineUninterceptedOrReturn(descriptor, typeMapper, languageVersionSettings)
        descriptor.isBuiltinAlwaysEnabledAssert() ->
            createMethodNodeForAlwaysEnabledAssert(descriptor, typeMapper)
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

private fun createSpecialEnumMethodBody(name: String, type: KotlinType, typeMapper: KotlinTypeMapper): MethodNode {
    val isValueOf = "enumValueOf" == name
    val invokeType = typeMapper.mapType(type)
    val desc = getSpecialEnumFunDescriptor(invokeType, isValueOf)
    val node = MethodNode(Opcodes.API_VERSION, Opcodes.ACC_STATIC, "fake", desc, null, null)
    ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameterWithoutPropagation(
        type,
        ReifiedTypeInliner.OperationKind.ENUM_REIFIED,
        InstructionAdapter(node)
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

private fun createTypeOfMethodBody(type: KotlinType): MethodNode {
    val node = MethodNode(Opcodes.API_VERSION, Opcodes.ACC_STATIC, "fake", Type.getMethodDescriptor(K_TYPE), null, null)
    val v = InstructionAdapter(node)

    putTypeOfReifiedTypeParameter(v, type)
    v.areturn(K_TYPE)

    v.visitMaxs(2, 0)

    return node
}

private fun putTypeOfReifiedTypeParameter(v: InstructionAdapter, type: KotlinType) {
    ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameterWithoutPropagation(type, ReifiedTypeInliner.OperationKind.TYPE_OF, v)
    v.aconst(null)
}

// Returns some upper bound on maximum stack size
internal fun generateTypeOf(v: InstructionAdapter, kotlinType: KotlinType, typeMapper: KotlinTypeMapper): Int {
    val asmType = typeMapper.mapType(kotlinType)
    AsmUtil.putJavaLangClassInstance(v, asmType, kotlinType, typeMapper)

    val arguments = kotlinType.arguments
    val useArray = arguments.size >= 3

    if (useArray) {
        v.iconst(arguments.size)
        v.newarray(K_TYPE_PROJECTION)
    }

    var maxStackSize = 3

    for (i in 0 until arguments.size) {
        if (useArray) {
            v.dup()
            v.iconst(i)
        }

        val stackSize = doGenerateTypeProjection(v, arguments[i], typeMapper)
        maxStackSize = maxOf(maxStackSize, stackSize + i + 5)

        if (useArray) {
            v.astore(K_TYPE_PROJECTION)
        }
    }

    val methodName = if (kotlinType.isMarkedNullable) "nullableTypeOf" else "typeOf"

    val projections = when (arguments.size) {
        0 -> emptyArray()
        1 -> arrayOf(K_TYPE_PROJECTION)
        2 -> arrayOf(K_TYPE_PROJECTION, K_TYPE_PROJECTION)
        else -> arrayOf(AsmUtil.getArrayType(K_TYPE_PROJECTION))
    }
    val signature = Type.getMethodDescriptor(K_TYPE, JAVA_CLASS_TYPE, *projections)

    v.invokestatic(REFLECTION, methodName, signature, false)

    return maxStackSize
}

private fun doGenerateTypeProjection(
    v: InstructionAdapter,
    projection: TypeProjection,
    typeMapper: KotlinTypeMapper
): Int {
    // KTypeProjection members could be static, see KT-30083 and KT-30084
    v.getstatic(K_TYPE_PROJECTION.internalName, "Companion", K_TYPE_PROJECTION_COMPANION.descriptor)

    if (projection.isStarProjection) {
        v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, "getSTAR", Type.getMethodDescriptor(K_TYPE_PROJECTION), false)
        return 1
    }

    val type = projection.type
    val descriptor = type.constructor.declarationDescriptor
    val stackSize = if (descriptor is TypeParameterDescriptor) {
        if (descriptor.isReified) {
            putTypeOfReifiedTypeParameter(v, type)
            2
        } else {
            // TODO: support non-reified type parameters in typeOf
            generateTypeOf(v, type.builtIns.nullableAnyType, typeMapper)
        }
    } else {
        generateTypeOf(v, type, typeMapper)
    }

    val methodName = when (projection.projectionKind) {
        Variance.INVARIANT -> "invariant"
        Variance.IN_VARIANCE -> "contravariant"
        Variance.OUT_VARIANCE -> "covariant"
    }
    v.invokevirtual(K_TYPE_PROJECTION_COMPANION.internalName, methodName, Type.getMethodDescriptor(K_TYPE_PROJECTION, K_TYPE), false)

    return stackSize + 1
}
