/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

/**
 * [KaPropertyAccessorSymbol] represents a [getter or setter declaration](https://kotlinlang.org/docs/properties.html#getters-and-setters)
 * of a property.
 */
public sealed class KaPropertyAccessorSymbol : KaFunctionSymbol() {
    override val isExtension: Boolean get() = withValidityAssertion { false }

    override val isExpect: Boolean get() = withValidityAssertion { false }

    /**
     * Property accessors cannot have the `actual` modifier in valid code. This modifier is not propagated from containing declarations (the
     * associated property) as for `expect` modifiers.
     */
    final override val isActual: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    /**
     * Whether the accessor is implicitly generated.
     */
    @Deprecated("Use `!isNotDefault` instead", ReplaceWith("!isNotDefault"))
    public abstract val isDefault: Boolean

    /**
     * Whether the accessor is [not default](https://kotlinlang.org/docs/properties.html#getters-and-setters).
     *
     * See also: [kotlin-metadata](https://kotlinlang.org/api/kotlinx-metadata-jvm/kotlin-metadata-jvm/kotlin.metadata/is-not-default.html).
     *
     * #### Example
     *
     * The following properties have implicitly declared accessors without a body:
     *
     * ```kotlin
     * class Foo(
     *   val bar: String // The generated property symbol has the default getter
     * )
     * ```
     *
     * The following properties have explicitly declared accessors without a body:
     *
     * ```kotlin
     * val foo: String = "Foo"
     *     @Throws(RuntimeException::class) get // The default getter annotated with `@Throws`.
     *
     * var bar: String = "Bar"
     *     private set // The default setter with private visibility.
     * ```
     *
     * The following properties have implicitly declared accessors with a body:
     *
     * ```kotlin
     * // The generated property accessor has a body
     * val foo by lazy { // not default
     *   0
     * }
     * ```
     *
     * The following properties have explicitly declared accessors with a body:
     *
     * ```kotlin
     * val foo
     *   // The getter has an expression body
     *   get() = "Foo" // not default
     *
     * var baz: Int = 0
     *   // The setter has a block body
     *   set(value) { // not default
     *     println(value)
     *   }
     * ```
     *
     * Property accessors from [KaSyntheticJavaPropertySymbol] are also not default.
     */
    public abstract val isNotDefault: Boolean

    /**
     * Whether the accessor is an [inline accessor](https://kotlinlang.org/docs/inline-functions.html#inline-properties).
     */
    public abstract val isInline: Boolean

    /**
     * Whether the accessor belongs to an [overriding property](https://kotlinlang.org/docs/inheritance.html#overriding-properties).
     */
    public abstract val isOverride: Boolean

    /**
     * Whether the accessor has a body. Declaring an accessor without a body allows to annotate it and to change its visibility, without
     * changing its default implementation.
     *
     * #### Example
     *
     * The following properties have explicit accessors without a body:
     *
     * ```kotlin
     * val foo: String = "Foo"
     *     @Throws(RuntimeException::class) get // The default getter annotated with `@Throws`.
     *
     * var bar: String = "Bar"
     *     private set // The default setter with private visibility.
     * ```
     */
    @Deprecated("Use `isCustom` instead", ReplaceWith("isCustom"))
    public abstract val hasBody: Boolean

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.PROPERTY }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyAccessorSymbol>
}

/**
 * [KaPropertyGetterSymbol] represents a property getter.
 *
 * @see KaPropertyAccessorSymbol
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaPropertyGetterSymbol : KaPropertyAccessorSymbol() {
    final override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    final override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol>
}

/**
 * [KaPropertyGetterSymbol] represents a property setter.
 *
 * @see KaPropertyAccessorSymbol
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public abstract class KaPropertySetterSymbol : KaPropertyAccessorSymbol() {
    /**
     * The single parameter of the setter representing the incoming value.
     */
    public abstract val parameter: KaValueParameterSymbol

    final override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol>
}
