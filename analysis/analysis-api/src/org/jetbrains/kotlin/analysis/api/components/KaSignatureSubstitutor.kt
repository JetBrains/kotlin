/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSignatureSubstitutor : KaLifetimeOwner {
    /**
     * Applies a [substitutor] to the given symbol and returns a [KaCallableSignature] with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.substitute' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.substitute(substitutor)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.substitute",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S>

    /**
     * Applies a [substitutor] to the given symbol and returns a [KaFunctionSignature] with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.substitute' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.substitute(substitutor)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.substitute",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S>

    /**
     * Applies a [substitutor] to the given symbol and returns a [KaVariableSignature] with substituted types.
     *
     * @see KaSubstitutor.substitute
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.substitute' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.substitute(substitutor)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.substitute",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S>

    /**
     * Creates a new [KaCallableSignature] for the given symbol and leaves all types unsubstituted.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.asSignature' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.asSignature()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.asSignature",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S>

    /**
     * Creates a new [KaFunctionSignature] for the given symbol and leaves all types unsubstituted.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.asSignature' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.asSignature()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.asSignature",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S>

    /**
     * Creates a new [KaVariableSignature] for the given symbol and leaves all types unsubstituted.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.signatures.asSignature' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.asSignature()",
            imports = [
                "org.jetbrains.kotlin.analysis.api.signatures.asSignature",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S>
}
