/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type

class IrTypeMapper(private val context: JvmBackendContext) {
    val kotlinTypeMapper: KotlinTypeMapper = context.state.typeMapper

    val classBuilderMode get() = kotlinTypeMapper.classBuilderMode

    fun classInternalName(irClass: IrClass) = kotlinTypeMapper.classInternalName(irClass.descriptor)

    fun mapAsmMethod(irFunction: IrFunction) = kotlinTypeMapper.mapAsmMethod(irFunction.descriptor)

    fun mapFieldSignature(irField: IrField) =
        kotlinTypeMapper.mapFieldSignature(irField.type.toKotlinType(), irField.descriptor)

    fun mapFunctionName(irReturnTarget: IrReturnTarget, ownerKind: OwnerKind) =
        kotlinTypeMapper.mapFunctionName(irReturnTarget.descriptor, ownerKind)

    fun mapImplementationOwner(irDeclaration: IrDeclaration) = kotlinTypeMapper.mapImplementationOwner(irDeclaration.descriptor)

    fun mapReturnType(irReturnTarget: IrReturnTarget) = kotlinTypeMapper.mapReturnType(irReturnTarget.descriptor)

    fun mapSignatureSkipGeneric(f: IrFunction, kind: OwnerKind = OwnerKind.IMPLEMENTATION) =
        kotlinTypeMapper.mapSignatureSkipGeneric(f.descriptor, kind)

    fun mapSignatureWithGeneric(f: IrFunction, kind: OwnerKind) = kotlinTypeMapper.mapSignatureWithGeneric(f.descriptor, kind)

    fun mapToCallableMethod(f: IrFunction, superCall: Boolean, kind: OwnerKind? = null, resolvedCall: ResolvedCall<*>? = null) =
        kotlinTypeMapper.mapToCallableMethod(f.descriptor, superCall, kind, resolvedCall)

    fun writeFormalTypeParameters(irParameters: List<IrTypeParameter>, sw: JvmSignatureWriter) =
        kotlinTypeMapper.writeFormalTypeParameters(irParameters.map { it.descriptor }, sw)

    fun boxType(irType: IrType) =
        AsmUtil.boxType(mapType(irType), irType.toKotlinType(), kotlinTypeMapper)

    fun mapType(
        type: IrType,
        mode: TypeMappingMode = TypeMappingMode.DEFAULT,
        sw: JvmSignatureWriter? = null
    ): Type =
        kotlinTypeMapper.mapType(type.toKotlinType(), sw, mode)
}
