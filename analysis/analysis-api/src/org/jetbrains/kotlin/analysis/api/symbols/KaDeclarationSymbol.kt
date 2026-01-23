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
     * The declaration's *own* [KaSymbolVisibility] (e.g. `public`).
     *
     * This is the visibility declared directly on the symbol itself, after applying all language rules and compiler plugins,
     * **but not** taking into account the visibility of any containing declarations.
     *
     * For example, a `public` function declared inside an `internal` class will still have [KaSymbolVisibility.PUBLIC] as its own visibility,
     * even though it is not accessible from outside the module due to its containing class being `internal`.
     *
     * #### Example
     *
     * ```kotlin
     * internal class InternalClass {
     *     fun implicitlyPublicFun() {}
     *     public fun explicitlyPublicFun() {}
     * }
     * ```
     *
     * In this example, both `implicitlyPublicFun` and `explicitlyPublicFun` have [KaSymbolVisibility.PUBLIC] as their own visibility
     * (the default visibility in Kotlin), even though they are effectively only accessible within the same module
     * because their containing class is `internal`.
     *
     * To check whether a declaration is actually visible from a specific use-site (taking into account the visibility of all containing declarations),
     * see [KaUseSiteVisibilityChecker.isVisible][org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker.isVisible].
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
     * Indicates whether this declaration is `external`.
     *
     * A declaration is considered external if any of the following is true:
     * - it is explicitly marked with the `external` modifier
     *   (applicable to classes, functions, properties, and property accessors);
     * - it is a member of an `external` class;
     * - it is a Java `native` method.
     *
     * External declarations have no Kotlin implementation and are expected to be
     * provided externally — via [JNI](https://kotlinlang.org/docs/java-interop.html#using-jni-with-kotlin) for the JVM target or
     * [JavaScript interop](https://kotlinlang.org/docs/js-interop.html#external-modifier) for the JS target.
     *
     * Other kinds of declarations (parameters, destructuring declarations, local
     * variables, etc.) are never considered external.
     *
     * #### Example
     *
     * ```kotlin
     * external class C {
     *     fun foo()
     *
     *     class Nested {
     *         fun bar()
     *     }
     * }
     *
     * external val baz: Int
     * ```
     *
     * In this example (which is legal on the JS target), `C`, `Nested`, `foo`, `bar`, and `baz` are all `external`.
     */
    public val isExternal: Boolean

    override fun createPointer(): KaSymbolPointer<KaDeclarationSymbol>
}

@KaExperimentalApi
public val KaDeclarationSymbol.typeParameters: List<KaTypeParameterSymbol>
    @OptIn(KaImplementationDetail::class)
    get() = if (this is KaTypeParameterOwnerSymbol) typeParameters else emptyList()
