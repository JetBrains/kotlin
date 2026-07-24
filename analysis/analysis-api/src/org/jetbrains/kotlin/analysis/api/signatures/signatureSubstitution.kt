/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

/**
 * Applies a [substitutor] to the given symbol and returns a [KaCallableSignature] with substituted types.
 *
 * @see KaSubstitutor.substitute
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaCallableSymbol> S.substitute(substitutor: KaSubstitutor): KaCallableSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.substitute(this, substitutor)
}

/**
 * Applies a [substitutor] to the given symbol and returns a [KaFunctionSignature] with substituted types.
 *
 * @see KaSubstitutor.substitute
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaFunctionSymbol> S.substitute(substitutor: KaSubstitutor): KaFunctionSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.substitute(this, substitutor)
}

/**
 * Applies a [substitutor] to the given symbol and returns a [KaVariableSignature] with substituted types.
 *
 * @see KaSubstitutor.substitute
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaVariableSymbol> S.substitute(substitutor: KaSubstitutor): KaVariableSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.substitute(this, substitutor)
}

/**
 * Creates a new [KaCallableSignature] for the given symbol and leaves all types unsubstituted.
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaCallableSymbol> S.asSignature(): KaCallableSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.asSignature(this)
}

/**
 * Creates a new [KaFunctionSignature] for the given symbol and leaves all types unsubstituted.
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.asSignature(this)
}

/**
 * Creates a new [KaVariableSignature] for the given symbol and leaves all types unsubstituted.
 */
@KaExperimentalApi
context(session: KaSession)
public fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S> {
    @OptIn(KaImplementationDetail::class)
    return internals.signatureSubstitutor.asSignature(this)
}
