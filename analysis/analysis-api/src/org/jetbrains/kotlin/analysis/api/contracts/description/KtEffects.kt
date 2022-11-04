/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

/**
 * K1: [org.jetbrains.kotlin.contracts.description.CallsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration]
 */
public class KtCallsEffectDeclaration(
    public val valueParameterReference: KtValueParameterReference,
    public val kind: EventOccurrencesRange
) : KtEffectDeclaration {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitCallsEffectDeclaration(this, data)
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration]
 */
public class KtConditionalEffectDeclaration(
    public val effect: KtEffectDeclaration,
    public val condition: KtBooleanExpression
) : KtEffectDeclaration {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitConditionalEffectDeclaration(this, data)
}

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ReturnsEffectDeclaration]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration]
 */
public class KtReturnsEffectDeclaration(public val value: KtConstantReference) : KtEffectDeclaration {
    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitReturnsEffectDeclaration(this, data)
}
