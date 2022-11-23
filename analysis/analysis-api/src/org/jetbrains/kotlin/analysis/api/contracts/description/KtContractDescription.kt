/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner

/**
 * This class hierarchy represents [kotlin.contracts] model.
 *
 * * K1: [org.jetbrains.kotlin.contracts.description.ContractDescriptionElement]
 * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionElement]
 */
public sealed interface KtContractDescriptionElement : KtLifetimeOwner

/**
 * Represents [kotlin.contracts.Effect].
 *
 * * K1: [org.jetbrains.kotlin.contracts.description.EffectDeclaration]
 * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeEffectDeclaration]
 */
public sealed interface KtContractEffectDeclaration : KtContractDescriptionElement

/**
 * Represents `booleanExpression` argument of [kotlin.contracts.SimpleEffect.implies]. `booleanExpression` forms a condition for
 * [KtContractConditionalContractEffectDeclaration]. See [KtContractConditionalContractEffectDeclaration.condition]
 *
 * * K1: [org.jetbrains.kotlin.contracts.description.BooleanExpression]
 * * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeBooleanExpression]
 */
public sealed interface KtContractBooleanExpression : KtContractDescriptionElement
