/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl.carriers

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

interface FunctionCarrier : FunctionBaseCarrier<FunctionCarrier> {
    var correspondingPropertySymbolField: IrPropertySymbol?
    var overriddenSymbolsField: List<IrSimpleFunctionSymbol>
    var attributeOwnerIdField: IrAttributeContainer

    override fun clone(): FunctionCarrier {
        return FunctionCarrierImpl(
            lastModified,
            parentField,
            originField,
            annotationsField,
            returnTypeFieldField,
            dispatchReceiverParameterField,
            extensionReceiverParameterField,
            bodyField,
            metadataField,
            visibilityField,
            typeParametersField,
            valueParametersField,
            correspondingPropertySymbolField,
            overriddenSymbolsField,
            attributeOwnerIdField
        )
    }
}

class FunctionCarrierImpl(
    override val lastModified: Int,
    override var parentField: IrDeclarationParent?,
    override var originField: IrDeclarationOrigin,
    override var annotationsField: List<IrConstructorCall>,
    override var returnTypeFieldField: IrType,
    override var dispatchReceiverParameterField: IrValueParameter?,
    override var extensionReceiverParameterField: IrValueParameter?,
    override var bodyField: IrBody?,
    override var metadataField: MetadataSource?,
    override var visibilityField: Visibility,
    override var typeParametersField: List<IrTypeParameter>,
    override var valueParametersField: List<IrValueParameter>,
    override var correspondingPropertySymbolField: IrPropertySymbol?,
    override var overriddenSymbolsField: List<IrSimpleFunctionSymbol>,
    override var attributeOwnerIdField: IrAttributeContainer
) : FunctionCarrier