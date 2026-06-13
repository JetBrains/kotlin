/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name

interface IrDeserializer : IrProvider {
    enum class TopLevelSymbolKind {
        FUNCTION_SYMBOL,
        CLASS_SYMBOL,
        PROPERTY_SYMBOL,
        TYPEALIAS_SYMBOL;
    }

    fun init(moduleFragment: IrModuleFragment?)

    /**
     * Retrieves the symbol associated with the given signature and kind.
     *
     * It is guaranteed that the returned symbol is not null if such a symbol belongs to the module represented by the current deserializer.
     * The returned symbol may be unbound (and put in the deserialization queue to be deserialized later).
     *
     * However, it is guaranteed that the returned symbol is always bound if [getSymbolAndPutIntoQueue] is called after the
     * main part of the deserialization process has been finished, i.e. after `postProcess(inOrAfterLinkageStep = true)` was called.
     *
     * @return The symbol for the given signature and kind if successfully found; otherwise, null.
     */
    fun getSymbolAndPutIntoQueue(signature: IdSignature, kind: TopLevelSymbolKind): IrSymbol?

    /**
     * [postProcess] has two usages with different expectations:
     * - IR plugin API: actualize expects/actuals, generate fake overrides
     * - Linker(s): the same + run partial linkage
     *
     * In the future, this function should be split into several functions with different semantics for more precise use.
     */
    @Deprecated(
        "Use postProcess(irBuiltIns, inOrAfterLinkageStep) instead",
        level = DeprecationLevel.ERROR
    )
    fun postProcess(): Nothing = error("Use postProcess(irBuiltIns, inOrAfterLinkageStep) instead")

    @Deprecated(
        "Use postProcess(irBuiltIns, inOrAfterLinkageStep) instead",
        level = DeprecationLevel.ERROR
    )
    fun postProcess(inOrAfterLinkageStep: Boolean): Nothing = error("Use postProcess(irBuiltIns, inOrAfterLinkageStep) instead")

    fun postProcess(irBuiltIns: IrBuiltIns, inOrAfterLinkageStep: Boolean)
}
