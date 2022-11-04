/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.contracts.description.expressions.BooleanVariableReference
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference

internal fun EffectDeclaration.effectDeclarationToAnalysisApi(): KtEffectDeclaration {
    return accept(EffectDeclarationToAnalysisApi, Unit)
}

private object EffectDeclarationToAnalysisApi : ContractDescriptionVisitor<KtEffectDeclaration, Unit> {
    override fun visitReturnsEffectDeclaration(returnsEffect: ReturnsEffectDeclaration, data: Unit): KtEffectDeclaration =
        KtReturnsEffectDeclaration()

    override fun visitCallsEffectDeclaration(callsEffect: CallsEffectDeclaration, data: Unit): KtEffectDeclaration =
        KtCallsKtEffectDeclaration(callsEffect.variableReference.accept(VariableReferenceToAnalysisApi, data), callsEffect.kind)

    override fun visitConditionalEffectDeclaration(conditionalEffect: ConditionalEffectDeclaration, data: Unit): KtEffectDeclaration =
        KtConditionalEffectDeclaration(conditionalEffect.effect.accept(this, data))
}

private object VariableReferenceToAnalysisApi : ContractDescriptionVisitor<KtValueParameterReference, Unit> {
    override fun visitVariableReference(variableReference: VariableReference, data: Unit): KtValueParameterReference =
        KtValueParameterReference(variableReference.descriptor.name.asString()) // todo check

    override fun visitBooleanVariableReference(booleanVariableReference: BooleanVariableReference, data: Unit): KtValueParameterReference =
        KtBooleanValueParameterReference(booleanVariableReference.descriptor.name.asString())
}
