/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeclarationDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrFlags
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.serialization.deserialization.ProtoEnumFlags
import org.jetbrains.kotlin.serialization.deserialization.descriptorVisibility

import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoIrConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoIrVariable

internal class IrCarrierDeserializerImpl(
    val declarationDeserializer: IrDeclarationDeserializer,
    val indexToBody: (Int) -> IrBody,
    val indexToExpressionBody: (Int) -> IrExpressionBody
) : IrCarrierDeserializer() {

    override fun deserializeParentSymbol(proto: Long): IrSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto)
    }

    override fun deserializeOrigin(proto: Int): IrDeclarationOrigin {
        return declarationDeserializer.deserializeIrDeclarationOrigin(proto)
    }

    override fun deserializeAnnotation(proto: ProtoIrConstructorCall): IrConstructorCall {
        return declarationDeserializer.bodyDeserializer.deserializeAnnotation(proto)
    }

    override fun deserializeBody(proto: Int): IrBody {
        return indexToBody(proto)
    }

    override fun deserializeBlockBody(proto: Int): IrBlockBody {
        return indexToBody(proto) as IrBlockBody
    }

    override fun deserializeExpressionBody(proto: Int): IrExpressionBody {
        return indexToExpressionBody(proto)
    }

    override fun deserializeValueParameter(proto: Long): IrValueParameterSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrValueParameterSymbol
    }

    override fun deserializeTypeParameter(proto: Long): IrTypeParameterSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrTypeParameterSymbol
    }

    override fun deserializeSuperType(proto: Int): IrType {
        return declarationDeserializer.deserializeIrType(proto)
    }

    override fun deserializeType(proto: Int): IrType {
        return declarationDeserializer.deserializeIrType(proto)
    }

    override fun deserializeClass(proto: Long): IrClassSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrClassSymbol
    }

    override fun deserializePropertySymbol(proto: Long): IrPropertySymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrPropertySymbol
    }

    override fun deserializeSimpleFunction(proto: Long): IrSimpleFunctionSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrSimpleFunctionSymbol
    }

    override fun deserializeSimpleFunctionSymbol(proto: Long): IrSimpleFunctionSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrSimpleFunctionSymbol
    }

    override fun deserializeField(proto: Long): IrFieldSymbol {
        return declarationDeserializer.symbolDeserializer.deserializeIrSymbol(proto) as IrFieldSymbol
    }

    override fun deserializeVariable(proto: ProtoIrVariable): IrVariable {
        return declarationDeserializer.deserializeIrVariable(proto)
    }

    override fun deserializeVisibility(proto: Long): DescriptorVisibility {
        return ProtoEnumFlags.descriptorVisibility(IrFlags.VISIBILITY.get(proto.toInt()))
    }

    override fun deserializeModality(proto: Long): Modality {
        return ProtoEnumFlags.modality(IrFlags.MODALITY.get(proto.toInt()))
    }

    override fun deserializeIsExternalClass(proto: Long): Boolean {
        return IrFlags.IS_EXTERNAL_CLASS.get(proto.toInt())
    }

    override fun deserializeIsExternalField(proto: Long): Boolean {
        return IrFlags.IS_EXTERNAL_FIELD.get(proto.toInt())
    }

    override fun deserializeIsExternalFunction(proto: Long): Boolean {
        return IrFlags.IS_EXTERNAL_FUNCTION.get(proto.toInt())
    }

    override fun deserializeIsExternalProperty(proto: Long): Boolean {
        return IrFlags.IS_EXTERNAL_PROPERTY.get(proto.toInt())
    }
}