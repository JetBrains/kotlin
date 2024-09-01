/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config.symbol

import org.jetbrains.kotlin.generators.tree.TypeRefWithNullability
import org.jetbrains.kotlin.generators.tree.config.AbstractElementConfigurator
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolField
import org.jetbrains.kotlin.ir.generator.obsoleteDescriptorBasedApiAnnotation
import org.jetbrains.kotlin.ir.generator.unsafeDuringIrConstructionApiAnnotation

abstract class AbstractIrSymbolTreeBuilder : AbstractElementConfigurator<Symbol, SymbolField, Nothing?>() {

    override fun createElement(name: String, propertyName: String, category: Nothing?): Symbol =
        Symbol(name, propertyName)

    protected fun field(
        name: String,
        type: TypeRefWithNullability,
        nullable: Boolean = false,
        mutable: Boolean = false,
        initializer: SymbolField.() -> Unit = {}
    ): SymbolField =
        SymbolField(name, type.copy(nullable), mutable).apply(initializer)

    protected fun ownerField(type: TypeRefWithNullability, initializer: SymbolField.() -> Unit = {}) =
        field("owner", type) {
            optInAnnotation = unsafeDuringIrConstructionApiAnnotation
            isOverride = true
            initializer()
        }

    private fun descriptorField(type: TypeRefWithNullability, initializer: SymbolField.() -> Unit = {}) =
        field("descriptor", type) {
            optInAnnotation = obsoleteDescriptorBasedApiAnnotation
            isOverride = true
            initializer()
        }

    protected fun descriptorField(descriptorClass: String, initializer: SymbolField.() -> Unit = {}) =
        descriptorField(type(Packages.descriptors, descriptorClass), initializer)
}