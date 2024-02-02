/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirValueParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.name.Name

class BirLazyValueParameter(
    override val originalIrElement: IrValueParameter,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirValueParameter, BirValueParameterSymbol {
    override val owner: BirValueParameter
        get() = this
    override val symbol: BirValueParameterSymbol
        get() = this

    override var index: Int
        get() = originalIrElement.index
        set(value) = mutationNotSupported()
    override var isCrossinline: Boolean
        get() = originalIrElement.isCrossinline
        set(value) = mutationNotSupported()
    override var isNoinline: Boolean
        get() = originalIrElement.isNoinline
        set(value) = mutationNotSupported()
    override var isHidden: Boolean
        get() = originalIrElement.isHidden
        set(value) = mutationNotSupported()
    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var isAssignable: Boolean
        get() = originalIrElement.isAssignable
        set(value) = mutationNotSupported()
    override var type: BirType by lazyVar<BirLazyValueParameter, _> {
        converter.remapType(originalIrElement.type)
    }
    override var varargElementType: BirType? by lazyVar<BirLazyValueParameter, _> {
        converter.remapType(originalIrElement.varargElementType)
    }
    private val _defaultValue = lazyVar<BirLazyValueParameter, _> {
        convertChild<BirExpressionBody?>(originalIrElement.defaultValue)
    }
    override var defaultValue: BirExpressionBody? by _defaultValue
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazyValueParameter, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
}