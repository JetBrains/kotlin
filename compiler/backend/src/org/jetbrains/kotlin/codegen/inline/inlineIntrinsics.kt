/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.common.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.createMethodNodeForAlwaysEnabledAssert
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructors
import org.jetbrains.kotlin.codegen.isBuiltinAlwaysEnabledAssert
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.calls.checkers.TypeOfChecker
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

fun generateInlineIntrinsicForIr(descriptor: FunctionDescriptor): SMAPAndMethodNode? =
    when {
        // TODO: implement these as codegen intrinsics (see IrIntrinsicMethods)
        descriptor.isBuiltInCoroutineContext() ->
            createMethodNodeForCoroutineContext(descriptor)
        descriptor.isBuiltInSuspendCoroutineUninterceptedOrReturn() ->
            createMethodNodeForSuspendCoroutineUninterceptedOrReturn()
        else -> null
    }?.let { SMAPAndMethodNode(it, SMAP(listOf())) }

internal fun generateInlineIntrinsic(
    descriptor: FunctionDescriptor,
    asmMethod: Method,
    typeSystem: TypeSystemCommonBackendContext
): SMAPAndMethodNode? {
    return generateInlineIntrinsicForIr(descriptor) ?: when {
        isSpecialEnumMethod(descriptor) ->
            createSpecialEnumMethodBody(descriptor.name.asString(), descriptor.original.typeParameters.single(), typeSystem)
        TypeOfChecker.isTypeOf(descriptor) ->
            typeSystem.createTypeOfMethodBody(descriptor.original.typeParameters.single())
        descriptor.isBuiltinAlwaysEnabledAssert() ->
            createMethodNodeForAlwaysEnabledAssert(descriptor)
        descriptor is FictitiousArrayConstructor ->
            IntrinsicArrayConstructors.generateArrayConstructorBody(asmMethod)
        IntrinsicArrayConstructors.isArrayOf(descriptor) ->
            IntrinsicArrayConstructors.generateArrayOfBody(asmMethod)
        IntrinsicArrayConstructors.isEmptyArray(descriptor) ->
            IntrinsicArrayConstructors.generateEmptyArrayBody(asmMethod)
        else -> null
    }?.let { SMAPAndMethodNode(it, SMAP(listOf())) }
}

private fun isSpecialEnumMethod(descriptor: FunctionDescriptor): Boolean {
    val containingDeclaration = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
    if (containingDeclaration.fqName != StandardNames.BUILT_INS_PACKAGE_FQ_NAME) {
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
        node.visitTypeInsn(Opcodes.CHECKCAST, ENUM_TYPE.internalName)
    } else {
        node.visitInsn(Opcodes.ICONST_0)
        node.visitTypeInsn(Opcodes.ANEWARRAY, ENUM_TYPE.internalName)
        node.visitTypeInsn(Opcodes.CHECKCAST, AsmUtil.getArrayType(ENUM_TYPE).internalName)
    }
    node.visitInsn(Opcodes.ARETURN)
    node.visitMaxs(if (isValueOf) 3 else 2, if (isValueOf) 1 else 0)
    return node
}

internal fun getSpecialEnumFunDescriptor(type: Type, isValueOf: Boolean): String =
    if (isValueOf) Type.getMethodDescriptor(type, JAVA_STRING_TYPE)
    else Type.getMethodDescriptor(AsmUtil.getArrayType(type))
