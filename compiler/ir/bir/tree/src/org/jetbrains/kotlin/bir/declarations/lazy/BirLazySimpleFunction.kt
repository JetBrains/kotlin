/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.Name

class BirLazySimpleFunction(
    override val originalIrElement: IrSimpleFunction,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirSimpleFunction, BirSimpleFunctionSymbol {
    override val owner: BirSimpleFunction
        get() = this
    override val symbol: BirSimpleFunctionSymbol
        get() = this

    override var isExternal: Boolean
        get() = originalIrElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalIrElement.visibility
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalIrElement.modality
        set(value) = mutationNotSupported()
    override var isTailrec: Boolean
        get() = originalIrElement.isTailrec
        set(value) = mutationNotSupported()
    override var isSuspend: Boolean
        get() = originalIrElement.isSuspend
        set(value) = mutationNotSupported()
    override var isFakeOverride: Boolean
        get() = originalIrElement.isFakeOverride
        set(value) = mutationNotSupported()
    override var isOperator: Boolean
        get() = originalIrElement.isOperator
        set(value) = mutationNotSupported()
    override var isInfix: Boolean
        get() = originalIrElement.isInfix
        set(value) = mutationNotSupported()
    override var isInline: Boolean
        get() = originalIrElement.isInline
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalIrElement.isExpect
        set(value) = mutationNotSupported()
    override var correspondingPropertySymbol: BirPropertySymbol? by lazyVar<BirLazySimpleFunction, _> {
        converter.remapSymbol(originalIrElement.correspondingPropertySymbol)
    }
    override var returnType: BirType by lazyVar<BirLazySimpleFunction, _> {
        converter.remapType(originalIrElement.returnType)
    }
    private val _dispatchReceiverParameter = lazyVar<BirLazySimpleFunction, _> {
        convertChild<BirValueParameter?>(originalIrElement.dispatchReceiverParameter)
    }
    override var dispatchReceiverParameter: BirValueParameter? by _dispatchReceiverParameter
    private val _extensionReceiverParameter = lazyVar<BirLazySimpleFunction, _> {
        convertChild<BirValueParameter?>(originalIrElement.extensionReceiverParameter)
    }
    override var extensionReceiverParameter: BirValueParameter? by _extensionReceiverParameter
    override var contextReceiverParametersCount: Int
        get() = originalIrElement.contextReceiverParametersCount
        set(value) = mutationNotSupported()
    private val _body = lazyVar<BirLazySimpleFunction, _> {
        convertChild<BirBody?>(originalIrElement.body)
    }
    override var body: BirBody? by _body
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazySimpleFunction, _> {
        converter.remapElement(originalIrElement.attributeOwnerId)
    }
    override var overriddenSymbols: List<BirSimpleFunctionSymbol> by lazyVar<BirLazySimpleFunction, _> {
        originalIrElement.overriddenSymbols.map { converter.remapSymbol(it) }
    }
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazySimpleFunction, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
    override val typeParameters = lazyChildElementList<BirLazySimpleFunction, BirTypeParameter>(2) { originalIrElement.typeParameters }
    override val valueParameters = lazyChildElementList<BirLazySimpleFunction, BirValueParameter>(3) { originalIrElement.valueParameters }
}