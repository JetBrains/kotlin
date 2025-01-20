/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.Name

/**
 * A [KaSymbol] with a [name].
 *
 * @see org.jetbrains.kotlin.analysis.api.symbols.name
 */
public interface KaNamedSymbol : KaSymbol {
    /**
     * The simple name of this [KaSymbol].
     *
     * For invalid declarations that are missing a name, [name] might be [SpecialNames.NO_NAME_PROVIDED][org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED].
     *
     * #### Example
     *
     * ```
     * package foo.bar
     *
     * fun baz() {
     * }
     * ```
     *
     * The [name] of the function `baz` is simply `baz`. In contrast, its [callableId][org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol.callableId]
     * is `foo/bar/baz`.
     */
    public val name: Name

    override fun createPointer(): KaSymbolPointer<KaNamedSymbol>
}

/**
 * A [KaSymbol] with [typeParameters].
 *
 * This class is an implementation detail and should not be used directly as a type. Consider using [typeParameters] directly from the
 * symbol, or [org.jetbrains.kotlin.analysis.api.symbols.typeParameters] for [KaDeclarationSymbol][org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol]s.
 *
 * @see org.jetbrains.kotlin.analysis.api.symbols.typeParameters
 */
@KaImplementationDetail
public interface KaTypeParameterOwnerSymbol : KaSymbol {
    public val typeParameters: List<KaTypeParameterSymbol>

    override fun createPointer(): KaSymbolPointer<KaTypeParameterOwnerSymbol>
}
