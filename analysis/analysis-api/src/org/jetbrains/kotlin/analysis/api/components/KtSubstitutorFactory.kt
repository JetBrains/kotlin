/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public abstract class KtSubstitutorFactory : KtAnalysisSessionComponent() {
    public abstract fun buildSubstitutor(builder: KtSubstitutorBuilder): KtSubstitutor
}

/**
 * Creates new [KtSubstitutor] using substitutions specified inside [build] lambda
 */
@OptIn(ExperimentalContracts::class, KtAnalysisApiInternals::class)
public inline fun KtAnalysisSession.buildSubstitutor(
    build: KtSubstitutorBuilder.() -> Unit,
): KtSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return analysisSession.substitutorFactory.buildSubstitutor(KtSubstitutorBuilder(token).apply(build))
}


public class KtSubstitutorBuilder
@KtAnalysisApiInternals constructor(override val token: KtLifetimeToken) : KtLifetimeOwner {
    private val backingMapping = mutableMapOf<KtTypeParameterSymbol, KtType>()

    public val mappings: Map<KtTypeParameterSymbol, KtType> get() = withValidityAssertion { backingMapping }

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor which is being built.
     * If there already was a substitution with a [typeParameter], replaces corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KtTypeParameterSymbol, type: KtType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    /**
     * Adds a new substitutions to the substitutor which is being built.
     * If there already was a substitution with a [KtTypeParameterSymbol] which is present in a [substitutions],
     * replaces corresponding substitution with a new one.
     */
    public fun substitutions(substitutions: Map<KtTypeParameterSymbol, KtType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}


