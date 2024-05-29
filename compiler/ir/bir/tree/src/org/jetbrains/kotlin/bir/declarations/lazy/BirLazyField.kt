/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirFieldSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.name.Name

class BirLazyField(
    override val originalIrElement: IrField,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirField, BirFieldSymbol {
    override val owner: BirField
        get() = this
    override val symbol: BirFieldSymbol
        get() = this

    override var isFinal: Boolean
        get() = originalIrElement.isFinal
        set(value) = mutationNotSupported()
    override var isStatic: Boolean
        get() = originalIrElement.isStatic
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
    override var type: BirType by lazyVar<BirLazyField, _> {
        converter.remapType(originalIrElement.type)
    }
    private val _initializer = lazyVar<BirLazyField, _> {
        convertChild<BirExpressionBody?>(originalIrElement.initializer)
    }
    override var initializer: BirExpressionBody? by _initializer
    override var correspondingPropertySymbol: BirPropertySymbol? by lazyVar<BirLazyField, _> {
        converter.remapSymbol(originalIrElement.correspondingPropertySymbol)
    }
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazyClass, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
}