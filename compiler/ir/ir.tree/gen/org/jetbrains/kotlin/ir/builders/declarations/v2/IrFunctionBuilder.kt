/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.declarations.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class IrFunctionBuilder internal constructor() : IrTypeParametersContainerBuilder() {
    var isExternal: Boolean = false

    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    var containerSource: DeserializedContainerSource? = null

    var metadata: MetadataSource? = null

    var isInline: Boolean = false

    var isExpect: Boolean = false

    var returnType: IrType? = null

    internal val parameters: MutableList<IrParameterBuilder> = []

    override fun copyFrom(from: IrDeclaration) {
        super.copyFrom(from)
        if (from is IrFunction) {
            isExternal = from.isExternal
            visibility = from.visibility
            containerSource = from.containerSource
            isInline = from.isInline
            isExpect = from.isExpect
            returnType = from.returnType
        }
    }

    fun dispatchReceiver(builder: IrDispatchReceiverBuilder.() -> Unit): IrValueParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrDispatchReceiverBuilder().apply(builder)
        parameters += child
        return child.symbol
    }

    fun contextParameter(builder: IrContextParameterBuilder.() -> Unit): IrValueParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrContextParameterBuilder().apply(builder)
        parameters += child
        return child.symbol
    }

    fun extensionReceiver(builder: IrExtensionReceiverBuilder.() -> Unit): IrValueParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrExtensionReceiverBuilder().apply(builder)
        parameters += child
        return child.symbol
    }

    fun valueParameter(builder: IrRegularParameterBuilder.() -> Unit): IrValueParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrRegularParameterBuilder().apply(builder)
        parameters += child
        return child.symbol
    }

    fun parameter(builder: IrValueParameterBuilder.() -> Unit): IrValueParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrValueParameterBuilder().apply(builder)
        parameters += child
        return child.symbol
    }

    fun valueParameter(name: Name, type: IrType): IrValueParameterSymbol =
        valueParameter {
            this.name = name
            this.type = type
        }

    fun valueParameter(name: String, type: IrType): IrValueParameterSymbol =
        valueParameter(Name.identifier(name), type)

    /**
     * Copies [source] parameters with their kinds and annotations, the types substituted with [substitution], then applies [adjust]
     * to each copy. Default values aren't copied. Returns the source parameters mapped to the copies.
     */
    fun copyParameters(
        source: List<IrValueParameter>,
        substitution: Map<IrTypeParameterSymbol, IrType> = emptyMap(),
        adjust: IrValueParameterBuilder.(from: IrValueParameter) -> Unit = {},
    ): Map<IrValueParameterSymbol, IrValueParameterSymbol> =
        buildParameterCopies(this, source, substitution, adjust)

    /**
     * Copies the type parameters, the parameters and the return type of [source], see [copyTypeParametersFrom] and [copyParameters].
     */
    fun copySignatureFrom(source: IrFunction): IrSignatureMapping =
        buildSignatureCopy(this, source)
}
