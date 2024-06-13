/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config.symbol

import org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolField
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolImplementation

abstract class AbstractIrSymbolTreeImplementationConfigurator :
    AbstractImplementationConfigurator<SymbolImplementation, Symbol, SymbolField, SymbolField>() {

    final override fun createImplementation(element: Symbol, name: String?): SymbolImplementation =
        SymbolImplementation(element, name)

    protected fun ImplementationContext.noSignature() {
        implementation.hasSignature = false
    }
}