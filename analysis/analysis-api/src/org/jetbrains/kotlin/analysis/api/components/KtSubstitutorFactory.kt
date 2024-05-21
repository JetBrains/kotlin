/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public abstract class KaSubstitutorFactory : KaSessionComponent() {
    public abstract fun buildSubstitutor(builder: KaSubstitutorBuilder): KaSubstitutor
}

public typealias KtSubstitutorFactory = KaSubstitutorFactory

/**
 * Creates new [KaSubstitutor] using substitutions specified inside [build] lambda
 */
@OptIn(ExperimentalContracts::class, KaAnalysisApiInternals::class)
public inline fun KaSession.buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return analysisSession.substitutorFactory.buildSubstitutor(KaSubstitutorBuilder(token).apply(build))
}


public class KaSubstitutorBuilder
@KaAnalysisApiInternals constructor(override val token: KaLifetimeToken) : KaLifetimeOwner {
    private val backingMapping = mutableMapOf<KaTypeParameterSymbol, KaType>()

    public val mappings: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { backingMapping }

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor which is being built.
     * If there already was a substitution with a [typeParameter], replaces corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    /**
     * Adds a new substitutions to the substitutor which is being built.
     * If there already was a substitution with a [KaTypeParameterSymbol] which is present in a [substitutions],
     * replaces corresponding substitution with a new one.
     */
    public fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}

public typealias KtSubstitutorBuilder = KaSubstitutorBuilder