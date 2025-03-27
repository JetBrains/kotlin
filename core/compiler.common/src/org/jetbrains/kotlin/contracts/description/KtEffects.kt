/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

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
class KtConditionalEffectDeclaration<Type, Diagnostic>(
    val effect: KtEffectDeclaration<Type, Diagnostic>,
    val condition: KtBooleanExpression<Type, Diagnostic>
) : KtEffectDeclaration<Type, Diagnostic>() {
    override val erroneous: Boolean
        get() = effect.erroneous || condition.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitConditionalEffectDeclaration(this, data)
}


/**
 * Returns effect with attached condition on arguments with it
 */
class KtConditionalReturnsDeclaration<Type, Diagnostic>(
    val argumentsCondition: KtBooleanExpression<Type, Diagnostic>,
    val returnsEffect: KtEffectDeclaration<Type, Diagnostic>
) : KtEffectDeclaration<Type, Diagnostic>() {
    override val erroneous: Boolean
        get() = argumentsCondition.erroneous || returnsEffect.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitConditionalReturnsDeclaration(this, data)
}


/**
 * Effect which specifies that subroutine returns some particular value
 */
class KtReturnsEffectDeclaration<Type, Diagnostic>(val value: KtConstantReference<Type, Diagnostic>) :
    KtEffectDeclaration<Type, Diagnostic>() {
    override val erroneous: Boolean
        get() = value.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitReturnsEffectDeclaration(this, data)
}


/**
 * Effect which specifies, that during execution of subroutine, callable [valueParameterReference] will be invoked
 * [kind] amount of times, and will never be invoked after subroutine call is finished.
 */
open class KtCallsEffectDeclaration<Type, Diagnostic>(
    val valueParameterReference: KtValueParameterReference<Type, Diagnostic>,
    val kind: EventOccurrencesRange
) : KtEffectDeclaration<Type, Diagnostic>() {
    override val erroneous: Boolean
        get() = valueParameterReference.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitCallsEffectDeclaration(this, data)
}


open class KtHoldsInEffectDeclaration<Type, Diagnostic>(
    val argumentsCondition: KtBooleanExpression<Type, Diagnostic>,
    val valueParameterReference: KtValueParameterReference<Type, Diagnostic>,
) : KtEffectDeclaration<Type, Diagnostic>() {
    override val erroneous: Boolean
        get() = argumentsCondition.erroneous || valueParameterReference.erroneous

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitHoldsInEffectDeclaration(this, data)
}

class KtErroneousCallsEffectDeclaration<Type, Diagnostic>(
    valueParameterReference: KtValueParameterReference<Type, Diagnostic>,
    val diagnostic: Diagnostic
) : KtCallsEffectDeclaration<Type, Diagnostic>(valueParameterReference, EventOccurrencesRange.UNKNOWN) {
    override val erroneous: Boolean
        get() = true

    override fun <R, D> accept(contractDescriptionVisitor: KtContractDescriptionVisitor<R, D, Type, Diagnostic>, data: D): R =
        contractDescriptionVisitor.visitErroneousCallsEffectDeclaration(this, data)
}
