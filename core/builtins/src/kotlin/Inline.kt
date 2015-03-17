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
 * Annotates the parameter of a function annotated as [inline] and forbids inlining of
 * function literals passed as arguments for this parameter.
 */
public annotation class noinline

/**
 * Enables inlining of the annotated function and the function literals that it takes as parameters into the
 * calling functions. Inline functions can use reified type parameters, and lambdas passed to inline
 * functions can contain non-local returns.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/inline-functions.html) for more information.
 *
 * @property strategy the [InlineStrategy] to use for inlining this function.
 *
 * @see noinline
 * @see inlineOptions
 */
public annotation class inline(public val strategy: InlineStrategy = InlineStrategy.AS_FUNCTION)

/**
 * Specifies the strategy for code generation for an inline function.
 */
public enum class InlineStrategy {
    /**
     * Specifies that the body of the inline function together with the bodies of the lambdas passed to it
     * is generated as a separate method invoked from the calling function.
     */
    AS_FUNCTION

    /**
     * Specifies that the body of the inline function together with the bodies of the lambdas passed to it
     * is inlined directly into the calling function.
     */
    IN_PLACE
}


/**
 * Specifies options for allowed control flow in inlined lambdas. Lambdas which are invoked directly by the
 * receiving function are allowed to use non-local control flow statements. Lambdas which are called from
 * a different execution context (for example, from an object contained in the receiving function)
 * are restricted to local control flow statements.
 *
 * @property value the inlining options selected for the annotated function parameter.
 */
public annotation class inlineOptions(vararg val value: InlineOption)

/**
 * Specifies the control flow statements which are allowed to be used for non-local control flow transfer in a lambda
 * passed as a parameter to an inline function.
 */
public enum class InlineOption {
    /**
     * This option hasn't been implemented yet.
     */
    LOCAL_CONTINUE_AND_BREAK

    /**
     * If this option is specified, lambdas may not use non-local return statements (statements which return from
     * their calling function or one of its enclosing functions). This option must be specified if the lambda
     * is not invoked directly by the receiving function but instead used in a different execution context
     * (for example, from an object contained in the receiving function).
     *
     * By default, lambdas are allowed to use non-local returns.
     */
    ONLY_LOCAL_RETURN
}
