/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.common.serialization.IrFlags
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility

import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoIrConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoIrVariable

internal class IrCarrierSerializerImpl(val fileSerializer: IrFileSerializer, val bodyIndex: (IrBody) -> Int) : IrCarrierSerializer() {
    override fun serializeParentSymbol(value: IrSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeOrigin(value: IrDeclarationOrigin): Int {
        return fileSerializer.serializeIrDeclarationOrigin(value)
    }

    override fun serializeAnnotation(value: IrConstructorCall): ProtoIrConstructorCall {
        return fileSerializer.serializeConstructorCall(value)
    }

    override fun serializeBody(value: IrBody): Int {
        return bodyIndex(value)
    }

    override fun serializeBlockBody(value: IrBlockBody): Int {
        return serializeBody(value)
    }

    override fun serializeExpressionBody(value: IrExpressionBody): Int {
        return serializeBody(value)
    }

    override fun serializeValueParameter(value: IrValueParameterSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeTypeParameter(value: IrTypeParameterSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeSuperType(value: IrType): Int {
        return fileSerializer.serializeIrType(value)
    }

    override fun serializeType(value: IrType): Int {
        return fileSerializer.serializeIrType(value)
    }

    override fun serializeClass(value: IrClassSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializePropertySymbol(value: IrPropertySymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeSimpleFunction(value: IrSimpleFunctionSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeSimpleFunctionSymbol(value: IrSimpleFunctionSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeField(value: IrFieldSymbol): Long {
        return fileSerializer.serializeIrSymbol(value)
    }

    override fun serializeVariable(value: IrVariable): ProtoIrVariable {
        return fileSerializer.serializeIrVariable(value)
    }

    override fun serializeVisibility(value: DescriptorVisibility): Long {
        return Flags.VISIBILITY.toFlags(ProtoEnumFlags.descriptorVisibility(value)).toLong()
    }

    override fun serializeModality(value: Modality): Long {
        return Flags.MODALITY.toFlags(ProtoEnumFlags.modality(value)).toLong()
    }

    override fun serializeIsExternalClass(value: Boolean): Long {
        return IrFlags.IS_EXTERNAL_CLASS.toFlags(value).toLong()
    }

    override fun serializeIsExternalField(value: Boolean): Long {
        return IrFlags.IS_EXTERNAL_FIELD.toFlags(value).toLong()
    }

    override fun serializeIsExternalFunction(value: Boolean): Long {
        return IrFlags.IS_EXTERNAL_FUNCTION.toFlags(value).toLong()
    }

    override fun serializeIsExternalProperty(value: Boolean): Long {
        return IrFlags.IS_EXTERNAL_PROPERTY.toFlags(value).toLong()
    }
}