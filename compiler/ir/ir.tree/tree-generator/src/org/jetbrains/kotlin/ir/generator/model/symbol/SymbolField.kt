/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model.symbol

import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.TypeRefWithNullability

/**
 * Represent a field of an [org.jetbrains.kotlin.ir.symbols.IrSymbol] subclass.
 */
class SymbolField(
    override val name: String,
    override val typeRef: TypeRefWithNullability,
    override var isMutable: Boolean,
) : AbstractField<SymbolField>() {

    override val origin: SymbolField
        get() = this

    override val isFinal: Boolean
        get() = false

    override var defaultValueInBuilder: String?
        get() = null
        set(_) = error("builders are not supported")

    override var customSetter: String?
        get() = null
        set(_) = error("setters are not supported")

    override val isChild: Boolean
        get() = false

    override fun replaceType(newType: TypeRefWithNullability): SymbolField =
        SymbolField(name, newType, isMutable).apply(this::updateFieldsInCopy)

    override fun copy(): SymbolField = replaceType(typeRef)
}