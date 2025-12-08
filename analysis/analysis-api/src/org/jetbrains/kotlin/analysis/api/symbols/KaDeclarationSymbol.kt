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
@OptIn(KaImplementationDetail::class)
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

    /**
     * Indicates whether the declaration is (effectively) external.
     *
     * A declaration is considered external if:
     * - it is marked with the `external` modifier, or
     * - it is contained in an `external` class or interface.
     *
     * External declarations are implemented outside of Kotlin (accessible through [JNI](https://kotlinlang.org/docs/java-interop.html#using-jni-with-kotlin)
     * or [JavaScript](https://kotlinlang.org/docs/js-interop.html#external-modifier)).
     *
     * #### Example
     *
     * ```kotlin
     * external class ExternalClass {
     *     fun foo() // effectively external
     * }
     *
     * external fun bar() // external
     * ```
     *
     * In this example, both `foo` and `bar` have `isExternal = true`.
     */
    public val isExternal: Boolean

    override fun createPointer(): KaSymbolPointer<KaDeclarationSymbol>
}

@KaExperimentalApi
public val KaDeclarationSymbol.typeParameters: List<KaTypeParameterSymbol>
    @OptIn(KaImplementationDetail::class)
    get() = if (this is KaTypeParameterOwnerSymbol) typeParameters else emptyList()
