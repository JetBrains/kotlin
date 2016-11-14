/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
 * Represents a function with introspection capabilities.
 */
public interface KFunction<out R> : KCallable<R>, Function<R> {
    /**
     * `true` if this function is `inline`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/inline-functions.html)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isInline: Boolean

    /**
     * `true` if this function is `external`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/java-interop.html#using-jni-with-kotlin)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isExternal: Boolean

    /**
     * `true` if this function is `operator`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/operator-overloading.html)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isOperator: Boolean

    /**
     * `true` if this function is `infix`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/functions.html#infix-notation)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isInfix: Boolean

    /**
     * `true` if this is a suspending function.
     */
    @SinceKotlin("1.1")
    public val isSuspend: Boolean
}
