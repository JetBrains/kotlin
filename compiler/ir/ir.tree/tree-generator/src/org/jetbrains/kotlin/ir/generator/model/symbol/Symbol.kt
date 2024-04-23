/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model.symbol

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.IrSymbolTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.model.Element

class Symbol(
    name: String,
    override val propertyName: String,
) : AbstractElement<Symbol, SymbolField, SymbolImplementation>(name) {

    override val namePrefix: String
        get() = "Ir"

    override val visitorParameterName: String
        get() = error("Visitors are not supported for IrSymbols")

    override val packageName: String
        get() = Packages.symbols

    override var kind: ImplementationKind?
        get() = if (isSealed) ImplementationKind.SealedInterface else ImplementationKind.Interface
        set(_) {}

    var descriptor: ClassRef<*>? = null

    var owner: Element? = null
}