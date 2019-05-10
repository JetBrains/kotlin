/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class IrTypeMapper(val kotlinTypeMapper: KotlinTypeMapper) {

    val classBuilderMode get() = kotlinTypeMapper.classBuilderMode

    fun classInternalName(irClass: IrClass) = kotlinTypeMapper.classInternalName(irClass.descriptor)

    fun mapAsmMethod(irFunction: IrFunction) = kotlinTypeMapper.mapAsmMethod(irFunction.descriptor)

    fun mapClass(irClass: IrClass) = kotlinTypeMapper.mapClass(irClass.descriptor)

    fun mapFieldSignature(irType: IrType, irFrield: IrField) =
        kotlinTypeMapper.mapFieldSignature(irType.toKotlinType(), irFrield.descriptor)

    fun mapFunctionName(irReturnTarget: IrReturnTarget, ownerKind: OwnerKind) =
        kotlinTypeMapper.mapFunctionName(irReturnTarget.descriptor, ownerKind)

    fun mapImplementationOwner(irDeclaration: IrDeclaration) = kotlinTypeMapper.mapImplementationOwner(irDeclaration.descriptor)

    fun mapReturnType(irReturnTarget: IrReturnTarget) = kotlinTypeMapper.mapReturnType(irReturnTarget.descriptor)

    fun mapSignatureSkipGeneric(f: IrFunction, kind: OwnerKind = OwnerKind.IMPLEMENTATION) =
        kotlinTypeMapper.mapSignatureSkipGeneric(f.descriptor, kind)

    fun mapSignatureWithGeneric(f: IrFunction, kind: OwnerKind) = kotlinTypeMapper.mapSignatureWithGeneric(f.descriptor, kind)

    fun mapSupertype(irType: IrType, sw: JvmSignatureWriter) = kotlinTypeMapper.mapSupertype(irType.toKotlinType(), sw)

    fun mapToCallableMethod(f: IrFunction, superCall: Boolean, kind: OwnerKind? = null, resolvedCall: ResolvedCall<*>? = null) =
        kotlinTypeMapper.mapToCallableMethod(f.descriptor, superCall, kind, resolvedCall)

    fun mapType(irType: IrType) = kotlinTypeMapper.mapType(irType.toKotlinType())

    fun mapType(irClass: IrClass) = kotlinTypeMapper.mapType(irClass.descriptor)

    fun mapType(irField: IrField) = kotlinTypeMapper.mapType(irField.descriptor)

    fun mapType(irValueParameter: IrValueParameter) = kotlinTypeMapper.mapType(irValueParameter.descriptor)

    fun mapType(irVariable: IrVariable) = kotlinTypeMapper.mapType(irVariable.descriptor)

    fun mapType(irType: IrType, sw: JvmSignatureWriter, mode: TypeMappingMode) =
        kotlinTypeMapper.mapType(irType.toKotlinType(), sw, mode)

    fun mapTypeAsDeclaration(irType: IrType) =
        kotlinTypeMapper.mapTypeAsDeclaration(irType.toKotlinType())

    fun mapTypeParameter(irType: IrType, signatureWriter: JvmSignatureWriter) =
        kotlinTypeMapper.mapTypeParameter(irType.toKotlinType(), signatureWriter)

    fun writeFormalTypeParameters(irParameters: List<IrTypeParameter>, sw: JvmSignatureWriter) =
        kotlinTypeMapper.writeFormalTypeParameters(irParameters.map { it.descriptor }, sw)

    fun boxType(irType: IrType) =
        AsmUtil.boxType(mapType(irType), irType.toKotlinType(), kotlinTypeMapper)
}
