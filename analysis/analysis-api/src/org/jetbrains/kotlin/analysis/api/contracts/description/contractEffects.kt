/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * Represents [kotlin.contracts.Effect].
 */
@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
public sealed interface KaContractEffectDeclaration : KaLifetimeOwner

/**
 * Represents [kotlin.contracts.ContractBuilder.callsInPlace].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractCallsInPlaceContractEffectDeclaration : KaContractEffectDeclaration {
    public val valueParameterReference: KaContractParameterValue

    @Deprecated("Use 'invocationKind' instead", level = DeprecationLevel.HIDDEN)
    public val occurrencesRange: EventOccurrencesRange

    /**
     * Describes how many times the [valueParameterReference] is invoked by a function.
     *
     * @see KaContractInvocationKind
     */
    public val invocationKind: KaContractInvocationKind
}

/**
 * Describes how many times a callable parameter is invoked by a function with a `callsInPlace` contract.
 */
public enum class KaContractInvocationKind {
    /** The parameter is never invoked. */
    ZERO,

    /** The parameter is invoked at most once. */
    AT_MOST_ONCE,

    /** The parameter is invoked exactly once. */
    EXACTLY_ONCE,

    /** The parameter is invoked at least once. */
    AT_LEAST_ONCE,

    /** The parameter is invoked more than once. */
    MORE_THAN_ONCE,

    /** The invocation count is unknown. */
    UNKNOWN,
}

/**
 * Represents [kotlin.contracts.SimpleEffect.implies].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractConditionalContractEffectDeclaration : KaContractEffectDeclaration {
    public val effect: KaContractEffectDeclaration
    public val condition: KaContractBooleanExpression
}

/**
 * Represents [kotlin.contracts.ContractBuilder.holdsIn].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractHoldsInEffectDeclaration : KaContractEffectDeclaration {
    public val condition: KaContractBooleanExpression
    public val valueParameterReference: KaContractParameterValue
}

/**
 * Represents [kotlin.contracts.ContractBuilder.returnsNotNull] & [kotlin.contracts.ContractBuilder.returns].
 */
@KaExperimentalApi
public sealed interface KaContractReturnsContractEffectDeclaration : KaContractEffectDeclaration {
    /**
     * Represents [kotlin.contracts.ContractBuilder.returnsNotNull].
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KaContractReturnsNotNullEffectDeclaration : KaContractReturnsContractEffectDeclaration

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] with a `value` argument.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KaContractReturnsSpecificValueEffectDeclaration : KaContractReturnsContractEffectDeclaration {
        public val value: KaContractConstantValue
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] without arguments.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface KaContractReturnsSuccessfullyEffectDeclaration : KaContractReturnsContractEffectDeclaration
}
