/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * Represents `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 *
 * `booleanExpression` forms a boolean condition for
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration]. See
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration.condition]
 */
@OptIn(KaImplementationDetail::class)
@KaExperimentalApi
public sealed interface KaContractBooleanExpression : KaLifetimeOwner

/**
 * Represents boolean parameter reference passed to `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractBooleanValueParameterExpression : KaContractBooleanExpression {
    public val parameterSymbol: KaContractParameterValue
}

/**
 * Represents boolean constant reference. The boolean constant can be passed to `booleanExpression` argument of
 * [kotlin.contracts.SimpleEffect.implies].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractBooleanConstantExpression : KaContractBooleanExpression {
    public val booleanConstant: Boolean
}
