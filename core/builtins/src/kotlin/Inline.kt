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

package kotlin

/**
 * Annotates the parameter of a function annotates as [inline] and forbids inlining of
 * function literals passed as arguments for this parameter.
 */
public annotation class noinline

/**
 * Enables inlining of the annotated function and the function literals that it takes as parameters into the
 * calling functions. Inline functions can use reified type parameters, and lambdas passed to inline
 * functions can contain non-local returns.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/inline-functions.html) for more information.
 */
public annotation class inline(public val strategy: InlineStrategy = InlineStrategy.AS_FUNCTION)

public enum class InlineStrategy {
    AS_FUNCTION
    IN_PLACE
}


public annotation class inlineOptions(vararg val value: InlineOption)

public enum class InlineOption {
    LOCAL_CONTINUE_AND_BREAK
    ONLY_LOCAL_RETURN
}
