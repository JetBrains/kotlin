/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model.symbol

import org.jetbrains.kotlin.generators.tree.AbstractImplementation
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.idSignatureType

class SymbolImplementation(
    symbol: Symbol,
    name: String?
) : AbstractImplementation<SymbolImplementation, Symbol, SymbolField>(symbol, name) {

    var generationCallback: (ImportCollectingPrinter.() -> Unit)? = null

    override val allFields: List<SymbolField>
        get() = emptyList()

    var hasSignature: Boolean = true
}