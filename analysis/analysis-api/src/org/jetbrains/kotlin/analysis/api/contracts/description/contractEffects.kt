/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * Represents [kotlin.contracts.Effect].
 */
@KaExperimentalApi
public sealed interface KaContractEffectDeclaration : KaLifetimeOwner

@Deprecated("Use 'KaContractEffectDeclaration' instead", ReplaceWith("KaContractEffectDeclaration"))
@KaExperimentalApi
public typealias KtContractEffectDeclaration = KaContractEffectDeclaration

/**
 * Represents [kotlin.contracts.ContractBuilder.callsInPlace].
 */
@KaExperimentalApi
public interface KaContractCallsInPlaceContractEffectDeclaration : KaContractEffectDeclaration {
    public val valueParameterReference: KaContractParameterValue
    public val occurrencesRange: EventOccurrencesRange
}

@Deprecated(
    "Use 'KaContractCallsInPlaceContractEffectDeclaration' instead",
    ReplaceWith("KaContractCallsInPlaceContractEffectDeclaration")
)
@KaExperimentalApi
public typealias KtContractCallsInPlaceContractEffectDeclaration = KaContractCallsInPlaceContractEffectDeclaration

/**
 * Represents [kotlin.contracts.SimpleEffect.implies].
 */
@KaExperimentalApi
public interface KaContractConditionalContractEffectDeclaration : KaContractEffectDeclaration {
    public val effect: KaContractEffectDeclaration
    public val condition: KaContractBooleanExpression
}

@Deprecated(
    "Use 'KaContractConditionalContractEffectDeclaration' instead",
    ReplaceWith("KaContractConditionalContractEffectDeclaration")
)
@KaExperimentalApi
public typealias KtContractConditionalContractEffectDeclaration = KaContractConditionalContractEffectDeclaration

/**
 * Represents [kotlin.contracts.ContractBuilder.returnsNotNull] & [kotlin.contracts.ContractBuilder.returns].
 */
@KaExperimentalApi
public sealed interface KaContractReturnsContractEffectDeclaration : KaContractEffectDeclaration {
    /**
     * Represents [kotlin.contracts.ContractBuilder.returnsNotNull].
     */
    public interface KaContractReturnsNotNullEffectDeclaration : KaContractReturnsContractEffectDeclaration

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] with a `value` argument.
     */
    public interface KaContractReturnsSpecificValueEffectDeclaration : KaContractReturnsContractEffectDeclaration {
        public val value: KaContractConstantValue
    }

    /**
     * Represents [kotlin.contracts.ContractBuilder.returns] without arguments.
     */
    public interface KaContractReturnsSuccessfullyEffectDeclaration : KaContractReturnsContractEffectDeclaration
}

@Deprecated("Use 'KaContractReturnsContractEffectDeclaration' instead", ReplaceWith("KaContractReturnsContractEffectDeclaration"))
@KaExperimentalApi
public typealias KtContractReturnsContractEffectDeclaration = KaContractReturnsContractEffectDeclaration
