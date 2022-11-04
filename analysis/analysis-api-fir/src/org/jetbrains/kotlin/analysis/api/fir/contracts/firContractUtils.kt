/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.contracts

import org.jetbrains.kotlin.analysis.api.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.*

internal fun ConeEffectDeclaration.coneEffectDeclarationToAnalysisApi(): KtEffectDeclaration =
    accept(ConeEffectDeclarationToAnalysisApi, Unit)

private object ConeEffectDeclarationToAnalysisApi : ConeContractDescriptionVisitor<KtEffectDeclaration, Unit>() {
    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Unit): KtEffectDeclaration =
        KtReturnsEffectDeclaration()

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Unit): KtEffectDeclaration =
        KtCallsKtEffectDeclaration(
            callsEffect.valueParameterReference.accept(ConeValueParameterReferenceToAnalysisApi, data),
            callsEffect.kind
        )

    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Unit
    ): KtEffectDeclaration = KtConditionalEffectDeclaration(conditionalEffect.effect.accept(this, data))
}

private object ConeValueParameterReferenceToAnalysisApi : ConeContractDescriptionVisitor<KtValueParameterReference, Unit>() {
    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Unit): KtValueParameterReference =
        KtValueParameterReference(valueParameterReference.name)

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: ConeBooleanValueParameterReference,
        data: Unit
    ): KtValueParameterReference = KtBooleanValueParameterReference(booleanValueParameterReference.name)
}
