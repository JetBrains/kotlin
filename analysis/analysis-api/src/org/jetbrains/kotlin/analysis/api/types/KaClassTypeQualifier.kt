/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.name.Name

/**
 * A type qualifier is a segment in a multi-segment class type application.
 *
 * Qualifiers become relevant when a type consists of multiple segments, such as `Foo.Bar`. Each qualifier in the list corresponds to a
 * segment, and allows retrieving the [KaClassifierSymbol] (if resolved), [name], and [typeArguments] at that position.
 *
 * A type qualifier is either [resolved][KaResolvedClassTypeQualifier] or [unresolved][KaUnresolvedClassTypeQualifier] in case of a type
 * error.
 *
 * #### Example
 *
 * ```kotlin
 * class Foo<T> {
 *     inner class Bar<U> { }
 * }
 *
 * val bar: Foo<Int>.Bar<String> = Foo<Int>().Bar<String>()
 * ```
 *
 * The type `Foo<Int>.Bar<String>` consists of two qualifiers, `Foo<Int>` and `Bar<String>`.
 */
public sealed interface KaClassTypeQualifier : KaLifetimeOwner {
    public val name: Name
    public val typeArguments: List<KaTypeProjection>
}

/**
 * A *successfully resolved* [KaClassTypeQualifier].
 */
public interface KaResolvedClassTypeQualifier : KaClassTypeQualifier {
    public val symbol: KaClassifierSymbol
}

/**
 * An *unresolved* [KaClassTypeQualifier] due to a type error.
 */
public interface KaUnresolvedClassTypeQualifier : KaClassTypeQualifier
