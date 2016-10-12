/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a declaration of a type parameter of a class or a callable.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/generics.html#generics)
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
