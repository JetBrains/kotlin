/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class MethodSignatureMapper(context: JvmBackendContext) {
    private val kotlinTypeMapper: KotlinTypeMapper = context.state.typeMapper

    fun mapAsmMethod(irFunction: IrFunction): Method =
        kotlinTypeMapper.mapAsmMethod(irFunction.descriptor)

    fun mapFieldSignature(irField: IrField): String? =
        kotlinTypeMapper.mapFieldSignature(irField.type.toKotlinType(), irField.descriptor)

    fun mapFunctionName(irFunction: IrFunction, ownerKind: OwnerKind?): String =
        kotlinTypeMapper.mapFunctionName(irFunction.descriptor, ownerKind)

    fun mapAnnotationParameterName(field: IrField): String =
        kotlinTypeMapper.mapAnnotationParameterName(field.descriptor)

    fun mapImplementationOwner(irDeclaration: IrDeclaration): Type =
        kotlinTypeMapper.mapImplementationOwner(irDeclaration.descriptor)

    fun mapReturnType(irField: IrField): Type =
        kotlinTypeMapper.mapReturnType(irField.descriptor)

    fun mapReturnType(irFunction: IrFunction): Type =
        kotlinTypeMapper.mapReturnType(irFunction.descriptor)

    fun mapSignatureSkipGeneric(f: IrFunction, kind: OwnerKind = OwnerKind.IMPLEMENTATION): JvmMethodSignature =
        kotlinTypeMapper.mapSignatureSkipGeneric(f.descriptor, kind)

    fun mapSignatureWithGeneric(f: IrFunction, kind: OwnerKind): JvmMethodGenericSignature =
        kotlinTypeMapper.mapSignatureWithGeneric(f.descriptor, kind)

    fun mapToCallableMethod(expression: IrFunctionAccessExpression, superCall: Boolean): IrCallableMethod =
        with(kotlinTypeMapper.mapToCallableMethod(expression.descriptor, superCall)) {
            IrCallableMethod(
                owner, valueParameterTypes, invokeOpcode, getAsmMethod(), dispatchReceiverType, extensionReceiverType, isInterfaceMethod
            )
        }
}
