/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.persistent

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.FunctionCarrier
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.ReturnTypeIsNotInitializedException
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal abstract class PersistentIrFunctionCommon(
    override val startOffset: Int,
    override val endOffset: Int,
    origin: IrDeclarationOrigin,
    override val name: Name,
    visibility: DescriptorVisibility,
    returnType: IrType,
    override val isInline: Boolean,
    override val isExternal: Boolean,
    override val isTailrec: Boolean,
    override val isSuspend: Boolean,
    override val isOperator: Boolean,
    override val isInfix: Boolean,
    override val isExpect: Boolean,
    override val containerSource: DeserializedContainerSource? = null
) : IrSimpleFunction(),
    PersistentIrDeclarationBase<FunctionCarrier>,
    FunctionCarrier {

    override var lastModified: Int = stageController.currentStage
    override var loweredUpTo: Int = stageController.currentStage
    override var values: Array<Carrier>? = null
    override val createdOn: Int = stageController.currentStage

    override var parentField: IrDeclarationParent? = null
    override var originField: IrDeclarationOrigin = origin
    override var removedOn: Int = Int.MAX_VALUE
    override var annotationsField: List<IrConstructorCall> = emptyList()

    override var returnTypeFieldField: IrType = returnType

    private var returnTypeField: IrType
        get() = getCarrier().returnTypeFieldField
        set(v) {
            if (returnTypeField !== v) {
                setCarrier().returnTypeFieldField = v
            }
        }

    final override var returnType: IrType
        get() = returnTypeField.let {
            if (it !== IrUninitializedType) it else throw ReturnTypeIsNotInitializedException(this)
        }
        set(c) {
            returnTypeField = c
        }

    override var typeParametersField: List<IrTypeParameter> = emptyList()

    override var typeParameters: List<IrTypeParameter>
        get() = getCarrier().typeParametersField
        set(v) {
            if (typeParameters !== v) {
                setCarrier().typeParametersField = v
            }
        }

    override var dispatchReceiverParameterField: IrValueParameter? = null

    override var dispatchReceiverParameter: IrValueParameter?
        get() = getCarrier().dispatchReceiverParameterField
        set(v) {
            if (dispatchReceiverParameter !== v) {
                setCarrier().dispatchReceiverParameterField = v
            }
        }

    override var extensionReceiverParameterField: IrValueParameter? = null

    override var extensionReceiverParameter: IrValueParameter?
        get() = getCarrier().extensionReceiverParameterField
        set(v) {
            if (extensionReceiverParameter !== v) {
                setCarrier().extensionReceiverParameterField = v
            }
        }

    override var valueParametersField: List<IrValueParameter> = emptyList()

    override var valueParameters: List<IrValueParameter>
        get() = getCarrier().valueParametersField
        set(v) {
            if (valueParameters !== v) {
                setCarrier().valueParametersField = v
            }
        }

    override var bodyField: IrBody? = null

    final override var body: IrBody?
        get() = getCarrier().bodyField
        set(v) {
            if (body !== v) {
                if (v is PersistentIrBodyBase<*>) {
                    v.container = this
                }
                setCarrier().bodyField = v
            }
        }

    override var metadataField: MetadataSource? = null

    override var metadata: MetadataSource?
        get() = getCarrier().metadataField
        set(v) {
            if (metadata !== v) {
                setCarrier().metadataField = v
            }
        }

    override var visibilityField: DescriptorVisibility = visibility

    override var visibility: DescriptorVisibility
        get() = getCarrier().visibilityField
        set(v) {
            if (visibility !== v) {
                setCarrier().visibilityField = v
            }
        }

    override var overriddenSymbolsField: List<IrSimpleFunctionSymbol> = emptyList()

    override var overriddenSymbols: List<IrSimpleFunctionSymbol>
        get() = getCarrier().overriddenSymbolsField
        set(v) {
            if (overriddenSymbols !== v) {
                setCarrier().overriddenSymbolsField = v
            }
        }

    @Suppress("LeakingThis")
    override var attributeOwnerIdField: IrAttributeContainer = this

    override var attributeOwnerId: IrAttributeContainer
        get() = getCarrier().attributeOwnerIdField
        set(v) {
            if (attributeOwnerId !== v) {
                setCarrier().attributeOwnerIdField = v
            }
        }

    override var correspondingPropertySymbolField: IrPropertySymbol? = null

    override var correspondingPropertySymbol: IrPropertySymbol?
        get() = getCarrier().correspondingPropertySymbolField
        set(v) {
            if (correspondingPropertySymbol !== v) {
                setCarrier().correspondingPropertySymbolField = v
            }
        }
}

internal class PersistentIrFunction(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val symbol: IrSimpleFunctionSymbol,
    name: Name,
    visibility: DescriptorVisibility,
    override val modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    isExpect: Boolean,
    override val isFakeOverride: Boolean = origin == IrDeclarationOrigin.FAKE_OVERRIDE,
    containerSource: DeserializedContainerSource?
) : PersistentIrFunctionCommon(
    startOffset, endOffset, origin, name, visibility, returnType, isInline,
    isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect,
    containerSource
) {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = symbol.descriptor

    init {
        symbol.bind(this)
    }
}

internal class PersistentIrFakeOverrideFunction(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    name: Name,
    override var visibility: DescriptorVisibility,
    override var modality: Modality,
    returnType: IrType,
    isInline: Boolean,
    isExternal: Boolean,
    isTailrec: Boolean,
    isSuspend: Boolean,
    isOperator: Boolean,
    isInfix: Boolean,
    isExpect: Boolean,
) : PersistentIrFunctionCommon(
    startOffset, endOffset, origin, name, visibility, returnType, isInline,
    isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect,
), IrFakeOverrideFunction {
    override val isFakeOverride: Boolean
        get() = true

    private var _symbol: IrSimpleFunctionSymbol? = null

    override val symbol: IrSimpleFunctionSymbol
        get() = _symbol ?: error("$this has not acquired a symbol yet")

    @ObsoleteDescriptorBasedAPI
    override val descriptor
        get() = _symbol?.descriptor ?: WrappedSimpleFunctionDescriptor()

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun acquireSymbol(symbol: IrSimpleFunctionSymbol): IrSimpleFunction {
        assert(_symbol == null) { "$this already has symbol _symbol" }
        _symbol = symbol
        symbol.bind(this)
        (symbol.descriptor as? WrappedSimpleFunctionDescriptor)?.bind(this)
        return this
    }
}
