/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Represents a declaration of a type parameter of a class or a callable.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#generics)
 * for more information.
 */
@SinceKotlin("1.1")
public interface KTypeParameter : KClassifier {
    /**
     * The name of this type parameter as it was declared in the source code.
     */
    public val name: String

    /**
     * Upper bounds, or generic constraints imposed on this type parameter.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#upper-bounds)
     * for more information.
     */
    public val upperBounds: List<KType>

    /**
     * Declaration-site variance of this type parameter.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#declaration-site-variance)
     * for more information.
     */
    public val variance: KVariance

    /**
     * `true` if this type parameter is `reified`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters)
     * for more information.
     */
    public val isReified: Boolean
}
