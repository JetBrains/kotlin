/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

object JsSymbolBuilder {
    fun buildValueParameter(
        containingSymbol: IrSimpleFunctionSymbol,
        index: Int,
        type: KotlinType,
        name: String? = null,
        hasDefault: Boolean = false
    ) =
        IrValueParameterSymbolImpl(
            ValueParameterDescriptorImpl(
                containingSymbol.descriptor,
                null,
                index,
                Annotations.EMPTY,
                Name.identifier(name ?: "param$index"),
                type,
                hasDefault,
                false,
                false,
                null,
                SourceElement.NO_SOURCE
            )
        )

    fun buildSimpleFunction(
        containingDeclaration: DeclarationDescriptor,
        name: String,
        annotations: Annotations = Annotations.EMPTY,
        kind: CallableMemberDescriptor.Kind = CallableMemberDescriptor.Kind.SYNTHESIZED,
        source: SourceElement = SourceElement.NO_SOURCE
    ) = IrSimpleFunctionSymbolImpl(
        SimpleFunctionDescriptorImpl.create(
            containingDeclaration,
            annotations,
            Name.identifier(name),
            kind,
            source
        )
    )

    fun copyFunctionSymbol(symbol: IrFunctionSymbol, newName: String) = IrSimpleFunctionSymbolImpl(
        SimpleFunctionDescriptorImpl.create(
            symbol.descriptor.containingDeclaration,
            symbol.descriptor.annotations,
            Name.identifier(newName),
            symbol.descriptor.kind,
            symbol.descriptor.source
        )
    )

    fun buildVar(
        containingSymbol: IrFunctionSymbol,
        name: String,
        type: KotlinType,
        mutable: Boolean = false
    ) = IrVariableSymbolImpl(
        LocalVariableDescriptor(
            containingSymbol.descriptor,
            Annotations.EMPTY,
            Name.identifier(name),
            type,
            mutable,
            false,
            SourceElement.NO_SOURCE
        )
    )
}


fun IrSimpleFunctionSymbol.initialize(
    receiverParameterType: KotlinType? = null,
    dispatchParameterDescriptor: ReceiverParameterDescriptor? = null,
    typeParameters: List<TypeParameterDescriptor> = emptyList(),
    valueParameters: List<ValueParameterDescriptor> = emptyList(),
    type: KotlinType? = null,
    modality: Modality = Modality.FINAL,
    visibility: Visibility = Visibilities.LOCAL
) = this.apply {
    (descriptor as FunctionDescriptorImpl).initialize(
        receiverParameterType,
        dispatchParameterDescriptor,
        typeParameters,
        valueParameters,
        type,
        modality,
        visibility
    )
}