/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature

interface IrDeserializer : IrProvider {
    enum class TopLevelSymbolKind {
        FUNCTION_SYMBOL,
        CLASS_SYMBOL,
        PROPERTY_SYMBOL,
        TYPEALIAS_SYMBOL;
    }

    /**
     * Retrieves the symbol associated with the given signature and kind.
     *
     * It is guaranteed that the returned symbol is not null if such a symbol belongs to the module represented by the current deserializer.
     * The returned symbol may be unbound (and put in the deserialization queue to be deserialized later).
     *
     * However, it is guaranteed that the returned symbol is always bound if [getSymbolAndPutIntoQueue] is called after the
     * main part of the deserialization process has been finished, i.e. after [KotlinIrLinker.finalizeIrLinkage] was called.
     *
     * @return The symbol for the given signature and kind if successfully found; otherwise, null.
     */
    fun getSymbolAndPutIntoQueue(signature: IdSignature, kind: TopLevelSymbolKind): IrSymbol?
}
