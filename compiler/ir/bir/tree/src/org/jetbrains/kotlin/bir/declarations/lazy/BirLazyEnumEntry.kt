/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.declarations.lazy

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.expressions.BirExpressionBody
import org.jetbrains.kotlin.bir.lazy.BirLazyElementBase
import org.jetbrains.kotlin.bir.symbols.BirEnumEntrySymbol
import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.name.Name

class BirLazyEnumEntry(
    override val originalIrElement: IrEnumEntry,
    converter: Ir2BirConverter,
) : BirLazyElementBase(converter), BirEnumEntry, BirEnumEntrySymbol {
    override val owner: BirEnumEntry
        get() = this
    override val symbol: BirEnumEntrySymbol
        get() = this

    override var name: Name
        get() = originalIrElement.name
        set(value) = mutationNotSupported()
    override var annotations: List<BirConstructorCall> by lazyVar<BirLazyEnumEntry, _> {
        originalIrElement.annotations.map { converter.remapElement(it) }
    }
    private val _correspondingClass = lazyVar<BirLazyEnumEntry, _> {
        converter.remapElement<BirClass>(originalIrElement.correspondingClass)
    }
    override var correspondingClass: BirClass? by _correspondingClass
    private val _initializerExpression = lazyVar<BirLazyEnumEntry, _> {
        convertChild<BirExpressionBody?>(originalIrElement.initializerExpression)
    }
    override var initializerExpression: BirExpressionBody? by _initializerExpression
}