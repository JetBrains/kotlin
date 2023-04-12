/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic

/**
 * Effect with condition attached to it.
 *
 * [condition] is some expression, which result-type is Boolean, and clause should
 * be interpreted as: "if [effect] took place then [condition]-expression is
 * guaranteed to be true"
 *
 * NB. [effect] and [condition] connected with implication in math logic sense:
 * [effect] => [condition]. In particular this means that:
 *  - there can be multiple ways how [effect] can be produced, but for any of them
 *    [condition] holds.
 *  - if [effect] wasn't observed, we *can't* reason that [condition] is false
 *  - if [condition] is true, we *can't* reason that [effect] will be observed.
 */
class ConeConditionalEffectDeclaration(val effect: ConeEffectDeclaration, val condition: ConeBooleanExpression) : ConeEffectDeclaration() {
    override val erroneous: Boolean
        get() = effect.erroneous || condition.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitConditionalEffectDeclaration(this, data)
}


/**
 * Effect which specifies that subroutine returns some particular value
 */
class ConeReturnsEffectDeclaration(val value: ConeConstantReference) : ConeEffectDeclaration() {
    override val erroneous: Boolean
        get() = value.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitReturnsEffectDeclaration(this, data)
}


/**
 * Effect which specifies, that during execution of subroutine, callable [valueParameterReference] will be invoked
 * [kind] amount of times, and will never be invoked after subroutine call is finished.
 */
open class ConeCallsEffectDeclaration(
    val valueParameterReference: ConeValueParameterReference,
    val kind: EventOccurrencesRange
) : ConeEffectDeclaration() {
    override val erroneous: Boolean
        get() = valueParameterReference.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitCallsEffectDeclaration(this, data)
}

class ConeErroneousCallsEffectDeclaration(
    valueParameterReference: ConeValueParameterReference,
    val diagnostic: ConeDiagnostic
) : ConeCallsEffectDeclaration(valueParameterReference, EventOccurrencesRange.UNKNOWN) {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: ConeContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitErroneousCallsEffectDeclaration(this, data)
}
