/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.Name

class BirLazySimpleFunction(
    override val originalElement: IrSimpleFunction,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirSimpleFunction {
    override val owner: BirSimpleFunction
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor
        get() = originalElement.descriptor
    override var isExternal: Boolean
        get() = originalElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalElement.visibility
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalElement.modality
        set(value) = mutationNotSupported()
    override var isTailrec: Boolean
        get() = originalElement.isTailrec
        set(value) = mutationNotSupported()
    override var isSuspend: Boolean
        get() = originalElement.isSuspend
        set(value) = mutationNotSupported()
    override var isFakeOverride: Boolean
        get() = originalElement.isFakeOverride
        set(value) = mutationNotSupported()
    override var isOperator: Boolean
        get() = originalElement.isOperator
        set(value) = mutationNotSupported()
    override var isInfix: Boolean
        get() = originalElement.isInfix
        set(value) = mutationNotSupported()
    override var isInline: Boolean
        get() = originalElement.isInline
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalElement.isExpect
        set(value) = mutationNotSupported()
    override var correspondingPropertySymbol: BirPropertySymbol? by lazyVar<BirLazySimpleFunction, _> {
        converter.remapSymbol(originalElement.correspondingPropertySymbol)
    }
    override var returnType: BirType by lazyVar<BirLazySimpleFunction, _> {
        converter.remapType(originalElement.returnType)
    }
    override var dispatchReceiverParameter: BirValueParameter? by lazyVar<BirLazySimpleFunction, _> {
        convertChild(originalElement.dispatchReceiverParameter)
    }
    override var extensionReceiverParameter: BirValueParameter? by lazyVar<BirLazySimpleFunction, _> {
        convertChild(originalElement.extensionReceiverParameter)
    }
    override var contextReceiverParametersCount: Int
        get() = originalElement.contextReceiverParametersCount
        set(value) = mutationNotSupported()
    override var body: BirBody? by lazyVar<BirLazySimpleFunction, _> {
        convertChild(originalElement.body)
    }
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazySimpleFunction, _> {
        converter.remapElement(originalElement.attributeOwnerId)
    }
    override var overriddenSymbols: List<BirSimpleFunctionSymbol> by lazyVar<BirLazySimpleFunction, _> {
        originalElement.overriddenSymbols.map { converter.remapSymbol(it) }
    }
    override val annotations = lazyChildElementList<BirLazySimpleFunction, BirConstructorCall>(1) { originalElement.annotations }
    override val typeParameters = lazyChildElementList<BirLazySimpleFunction, BirTypeParameter>(2) { originalElement.typeParameters }
    override val valueParameters = lazyChildElementList<BirLazySimpleFunction, BirValueParameter>(3) { originalElement.valueParameters }
}