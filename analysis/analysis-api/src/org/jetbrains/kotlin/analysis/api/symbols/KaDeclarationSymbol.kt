/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.Visibility

/**
 * Represents a source-representable declaration such as a class, function, or property.
 *
 * Files and packages are not considered [KaDeclarationSymbol]s, as they cannot be declared explicitly in one place. For example, a
 * [KaPackageSymbol] is the semantic representation of a package made up of possibly multiple Kotlin source files.
 */
public sealed interface KaDeclarationSymbol : KaSymbol, KaAnnotatedSymbol {
    /**
     * The declaration's *effective* [KaSymbolModality] (e.g. `open`). Effective modality is the symbol's modality after all language rules
     * and compiler plugins have been taken into account, in contrast to the syntactic modality.
     */
    public val modality: KaSymbolModality

    /**
     * The declaration's *effective* [KaSymbolVisibility] (e.g. `public`). Effective visibility is the symbol's visibility after all
     * language rules and compiler plugins have been taken into account, in contrast to the syntactic visibility.
     */
    public val visibility: KaSymbolVisibility
        @OptIn(KaExperimentalApi::class)
        get() = compilerVisibility.asKaSymbolVisibility

    @KaExperimentalApi
    public val compilerVisibility: Visibility

    /**
     * Whether the declaration is an `actual` declaration in a multiplatform project.
     *
     * See [the official Kotlin documentation](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html) for more details.
     */
    public val isActual: Boolean

    /**
     * Whether the declaration is an `expect` declaration in a multiplatform project.
     *
     * See [the official Kotlin documentation](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html) for more details.
     *
     * #### Example
     *
     * ```kotlin
     * expect class A {
     *     class Nested
     * }
     * ```
     *
     * In this example, `isExpect` is `true` for both `A` and `A.Nested`.
     */
    public val isExpect: Boolean

    override fun createPointer(): KaSymbolPointer<KaDeclarationSymbol>
}

@KaExperimentalApi
public val KaDeclarationSymbol.typeParameters: List<KaTypeParameterSymbol>
    @OptIn(KaImplementationDetail::class)
    get() = if (this is KaTypeParameterOwnerSymbol) typeParameters else emptyList()
