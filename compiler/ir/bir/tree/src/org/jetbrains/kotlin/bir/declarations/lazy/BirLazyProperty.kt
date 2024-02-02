/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.Name

class BirLazyProperty(
    override val originalIrElement: IrProperty,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirProperty, BirPropertySymbol {
    override val owner: BirProperty
        get() = this
    override val symbol: BirPropertySymbol
        get() = this

    override var isExternal: Boolean
        get() = originalIrElement.isExternal
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var modality: Modality
        get() = originalIrElement.modality
        set(value) = mutationNotSupported()
    override var visibility: DescriptorVisibility
        get() = originalIrElement.visibility
        set(value) = mutationNotSupported()
    override var isVar: Boolean
        get() = originalIrElement.isVar
        set(value) = mutationNotSupported()
    override var isConst: Boolean
        get() = originalIrElement.isConst
        set(value) = mutationNotSupported()
    override var isLateinit: Boolean
        get() = originalIrElement.isLateinit
        set(value) = mutationNotSupported()
    override var isDelegated: Boolean
        get() = originalIrElement.isDelegated
        set(value) = mutationNotSupported()
    override var isExpect: Boolean
        get() = originalIrElement.isExpect
        set(value) = mutationNotSupported()
    override var isFakeOverride: Boolean
        get() = originalIrElement.isFakeOverride
        set(value) = mutationNotSupported()
    override var attributeOwnerId: BirAttributeContainer by lazyVar<BirLazyProperty, _> {
        converter.remapElement(originalIrElement.attributeOwnerId)
    }
    private val _backingField = lazyVar<BirLazyProperty, _> {
        convertChild<BirField?>(originalIrElement.backingField)
    }
    override var backingField: BirField? by _backingField
    private val _getter = lazyVar<BirLazyProperty, _> {
        convertChild<BirSimpleFunction?>(originalIrElement.getter)
    }
    override var getter: BirSimpleFunction? by _getter
    private val _setter = lazyVar<BirLazyProperty, _> {
        convertChild<BirSimpleFunction?>(originalIrElement.setter)
    }
    override var setter: BirSimpleFunction? by _setter
    override var overriddenSymbols: List<BirPropertySymbol> by lazyVar<BirLazyProperty, _> {
        originalIrElement.overriddenSymbols.map { converter.remapSymbol(it) }
    }
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazyProperty, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
}