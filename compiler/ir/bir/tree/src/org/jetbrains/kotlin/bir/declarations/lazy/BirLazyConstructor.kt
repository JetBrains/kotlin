/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirBody
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.lazy.acceptLiteIfPresent
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.name.Name

class BirLazyConstructor(
    override val originalIrElement: IrConstructor,
    converter: Ir2BirConverter,
) : BirLazyElementBase(BirConstructor, converter), BirConstructor {
    override val owner: BirConstructor
        get() = this

    override var isPrimary: Boolean
        get() = originalIrElement.isPrimary
        set(value) = mutationNotSupported()
    override var isExternal: Boolean
        get() = originalIrElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalIrElement.visibility
        set(value) = mutationNotSupported()
    override var isInline: Boolean
        get() = originalIrElement.isInline
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalIrElement.isExpect
        set(value) = mutationNotSupported()
    override var contextReceiverParametersCount: Int
        get() = originalIrElement.contextReceiverParametersCount
        set(value) = mutationNotSupported()
    override var returnType: BirType by lazyVar<BirLazyConstructor, _> {
        converter.remapType(originalIrElement.returnType)
    }
    private val _dispatchReceiverParameter = lazyVar<BirLazyConstructor, _> {
        convertChild<BirValueParameter?>(originalIrElement.dispatchReceiverParameter)
    }
    override var dispatchReceiverParameter: BirValueParameter? by _dispatchReceiverParameter
    private val _extensionReceiverParameter = lazyVar<BirLazyConstructor, _> {
        convertChild<BirValueParameter?>(originalIrElement.extensionReceiverParameter)
    }
    override var extensionReceiverParameter: BirValueParameter? by _extensionReceiverParameter
    private val _body = lazyVar<BirLazyConstructor, _> {
        convertChild<BirBody?>(originalIrElement.body)
    }
    override var body: BirBody? by _body
    override val annotations = lazyChildElementList<BirLazyConstructor, BirConstructorCall>(1) { originalIrElement.annotations }
    override val typeParameters = lazyChildElementList<BirLazyConstructor, BirTypeParameter>(2) { originalIrElement.typeParameters }
    override val valueParameters = lazyChildElementList<BirLazyConstructor, BirValueParameter>(3) { originalIrElement.valueParameters }
}