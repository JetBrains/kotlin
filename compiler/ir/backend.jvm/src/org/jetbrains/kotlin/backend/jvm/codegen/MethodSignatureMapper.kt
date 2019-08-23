/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.hasJvmDefault
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class MethodSignatureMapper(context: JvmBackendContext) {
    private val typeMapper: IrTypeMapper = context.typeMapper
    private val kotlinTypeMapper: KotlinTypeMapper = context.state.typeMapper

    fun mapAsmMethod(irFunction: IrFunction): Method =
        kotlinTypeMapper.mapAsmMethod(irFunction.descriptor)

    fun mapFieldSignature(irField: IrField): String? =
        kotlinTypeMapper.mapFieldSignature(irField.type.toKotlinType(), irField.descriptor)

    fun mapFunctionName(irFunction: IrFunction, ownerKind: OwnerKind?): String =
        kotlinTypeMapper.mapFunctionName(irFunction.descriptor, ownerKind)

    fun mapAnnotationParameterName(field: IrField): String =
        kotlinTypeMapper.mapAnnotationParameterName(field.descriptor)

    fun mapReturnType(irField: IrField): Type =
        kotlinTypeMapper.mapReturnType(irField.descriptor)

    fun mapReturnType(irFunction: IrFunction): Type =
        kotlinTypeMapper.mapReturnType(irFunction.descriptor)

    fun mapSignatureSkipGeneric(f: IrFunction, kind: OwnerKind = OwnerKind.IMPLEMENTATION): JvmMethodSignature =
        kotlinTypeMapper.mapSignatureSkipGeneric(f.descriptor, kind)

    fun mapSignatureWithGeneric(f: IrFunction, kind: OwnerKind): JvmMethodGenericSignature =
        kotlinTypeMapper.mapSignatureWithGeneric(f.descriptor, kind)

    fun mapToCallableMethod(expression: IrFunctionAccessExpression): IrCallableMethod {
        val callee = expression.symbol.owner
        val calleeParent = callee.parent
        if (calleeParent !is IrClass) {
            // Non-class parent is only possible for intrinsics created in IrBuiltIns, such as dataClassArrayMemberHashCode. In that case,
            // we still need to return some IrCallableMethod with some owner instance, but that owner will be ignored at the call site.
            // Here we return a fake type, but this needs to be refactored so that we never call mapToCallableMethod on intrinsics.
            // TODO: get rid of fake owner here
            val fakeOwner = Type.getObjectType("kotlin/internal/ir/Intrinsic")
            return IrCallableMethod(fakeOwner, Opcodes.INVOKESTATIC, mapSignatureSkipGeneric(callee), false)
        }

        val owner = typeMapper.mapClass(calleeParent)

        if (callee !is IrSimpleFunction) {
            check(callee is IrConstructor) { "Function must be a simple function or a constructor: ${callee.render()}" }
            return IrCallableMethod(owner, Opcodes.INVOKESPECIAL, mapSignatureSkipGeneric(callee), false)
        }

        val isInterface = calleeParent.isJvmInterface
        val isSuperCall = (expression as? IrCall)?.superQualifier != null

        val invokeOpcode = when {
            callee.dispatchReceiverParameter == null -> Opcodes.INVOKESTATIC
            isSuperCall -> Opcodes.INVOKESPECIAL
            isInterface -> Opcodes.INVOKEINTERFACE
            Visibilities.isPrivate(callee.visibility) && !callee.isSuspend -> Opcodes.INVOKESPECIAL
            else -> Opcodes.INVOKEVIRTUAL
        }

        val declaration = findSuperDeclaration(callee, isSuperCall)
        val signature = mapOverriddenSpecialBuiltinIfNeeded(declaration, isSuperCall)
            ?: mapSignatureSkipGeneric(declaration)

        return IrCallableMethod(owner, invokeOpcode, signature, isInterface)
    }

    // TODO: get rid of this (probably via some special lowering)
    private fun mapOverriddenSpecialBuiltinIfNeeded(callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        val overriddenSpecialBuiltinFunction = callee.descriptor.original.getOverriddenBuiltinReflectingJvmDescriptor()
        if (overriddenSpecialBuiltinFunction != null && !superCall) {
            return kotlinTypeMapper.mapSignatureSkipGeneric(overriddenSpecialBuiltinFunction.original, OwnerKind.IMPLEMENTATION)
        }

        return null
    }

    // Copied from KotlinTypeMapper.findSuperDeclaration.
    private fun findSuperDeclaration(function: IrSimpleFunction, isSuperCall: Boolean): IrSimpleFunction {
        var current = function
        while (current.isFakeOverride) {
            // TODO: probably isJvmInterface instead of isInterface, here and in KotlinTypeMapper
            val classCallable = current.overriddenSymbols.firstOrNull { !it.owner.parentAsClass.isInterface }?.owner
            if (classCallable != null) {
                current = classCallable
                continue
            }
            if (isSuperCall && !current.hasJvmDefault() && !current.parentAsClass.isInterface) {
                return current
            }

            current = current.overriddenSymbols.firstOrNull()?.owner
                ?: error("Fake override should have at least one overridden descriptor: ${current.render()}")
        }
        return current
    }
}
