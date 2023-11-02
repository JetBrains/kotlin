/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.name.Name

class BirLazyField(
    override val originalElement: IrField,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirField {
    override val owner: BirField
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: PropertyDescriptor
        get() = originalElement.descriptor
    override var isFinal: Boolean
        get() = originalElement.isFinal
        set(value) = mutationNotSupported()
    override var isStatic: Boolean
        get() = originalElement.isStatic
        set(value) = mutationNotSupported()
    override var isExternal: Boolean
        get() = originalElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalElement.name
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalElement.visibility
        set(value) = mutationNotSupported()
    override var type: BirType by lazyVar<BirLazyField, _> {
        converter.remapType(originalElement.type)
    }
    override var initializer: BirExpressionBody? by lazyVar<BirLazyField, _> {
        convertChild(originalElement.initializer)
    }
    override var correspondingPropertySymbol: BirPropertySymbol? by lazyVar<BirLazyField, _> {
        converter.remapSymbol(originalElement.correspondingPropertySymbol)
    }
    override val annotations = lazyChildElementList<BirLazyField, BirConstructorCall>(1) { originalElement.annotations }
}