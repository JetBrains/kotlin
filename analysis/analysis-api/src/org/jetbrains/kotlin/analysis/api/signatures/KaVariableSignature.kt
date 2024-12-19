/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.name.Name

/**
 * A [callable signature][KaCallableSignature] of a [variable symbol][KaVariableSymbol].
 */
public interface KaVariableSignature<out S : KaVariableSymbol> : KaCallableSignature<S> {
    /**
     * The name of the variable with respect to a [ParameterName] annotation. It can be different from [KaVariableSymbol.name].
     *
     * Some variables can have their names changed by special annotations like `@ParameterName(name = "newName")`. This is used to preserve
     * the names of the lambda parameters in situations like this:
     *
     * ```
     * // compiled library
     * fun foo(): (bar: String) -> Unit { ... }
     *
     * // source code
     * fun test() {
     *   val action = foo()
     *   action("") // this call
     * }
     * ```
     *
     * Unfortunately, the [symbol] for the `action("")` call will be pointing to `Function1<P1, R>.invoke(p1: P1): R`, because we
     * intentionally unwrap use-site substitution overrides. Because of this, `symbol.name` will yield `"p1"`, and not `"bar"`.
     *
     * To overcome this problem, [name] allows to get the intended name of the parameter, with respect to the `@ParameterName` annotation.
     */
    public val name: Name

    @KaExperimentalApi
    abstract override fun substitute(substitutor: KaSubstitutor): KaVariableSignature<S>
}
