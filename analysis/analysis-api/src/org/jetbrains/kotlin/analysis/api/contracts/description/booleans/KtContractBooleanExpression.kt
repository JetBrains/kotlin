/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

/**
 * Represents `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 *
 * `booleanExpression` forms a boolean condition for
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration]. See
 * [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration.condition]
 */
public sealed interface KaContractBooleanExpression : KaLifetimeOwner

@Deprecated("Use 'KaContractBooleanExpression' instead", ReplaceWith("KaContractBooleanExpression"))
public typealias KtContractBooleanExpression = KaContractBooleanExpression

/**
 * Represents boolean parameter reference passed to `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies].
 */
public interface KaContractBooleanValueParameterExpression : KaContractBooleanExpression {
    public val parameterSymbol: KaParameterSymbol
}

@Deprecated("Use 'KaContractBooleanValueParameterExpression' instead", ReplaceWith("KaContractBooleanValueParameterExpression"))
public typealias KtContractBooleanValueParameterExpression = KaContractBooleanValueParameterExpression

/**
 * Represents boolean constant reference. The boolean constant can be passed to `booleanExpression` argument of
 * [kotlin.contracts.SimpleEffect.implies].
 */
public interface KaContractBooleanConstantExpression : KaContractBooleanExpression {
    public val booleanConstant: Boolean
}

@Deprecated("Use 'KaContractBooleanConstantExpression' instead", ReplaceWith("KaContractBooleanConstantExpression"))
public typealias KtContractBooleanConstantExpression = KaContractBooleanConstantExpression
