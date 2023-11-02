/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.Name

class BirLazyProperty(
    override val originalElement: IrProperty,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirProperty {
    override val owner: BirProperty
        get() = this
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: PropertyDescriptor
        get() = originalElement.descriptor
    override var isExternal: Boolean
        get() = originalElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalElement.name
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalElement.modality
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalElement.visibility
        set(value) = mutationNotSupported()
    override var isVar: Boolean
        get() = originalElement.isVar
        set(value) = mutationNotSupported()
    override var isConst: Boolean
        get() = originalElement.isConst
        set(value) = mutationNotSupported()
    override var isLateinit: Boolean
        get() = originalElement.isLateinit
        set(value) = mutationNotSupported()
    override var isDelegated: Boolean
        get() = originalElement.isDelegated
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalElement.isExpect
        set(value) = mutationNotSupported()
    override var isFakeOverride: Boolean
        get() = originalElement.isFakeOverride
        set(value) = mutationNotSupported()
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazyProperty, _> {
        converter.remapElement(originalElement.attributeOwnerId)
    }
    override var backingField: BirField? by lazyVar<BirLazyProperty, _> {
        converter.remapElement(originalElement.backingField)
    }
    override var getter: BirSimpleFunction? by lazyVar<BirLazyProperty, _> {
        convertChild(originalElement.getter)
    }
    override var setter: BirSimpleFunction? by lazyVar<BirLazyProperty, _> {
        convertChild(originalElement.setter)
    }
    override var overriddenSymbols: List<BirPropertySymbol> by lazyVar<BirLazyProperty, _> {
        originalElement.overriddenSymbols.map { converter.remapSymbol(it) }
    }
    override val annotations = lazyChildElementList<BirLazyProperty, BirConstructorCall>(1) { originalElement.annotations }
}