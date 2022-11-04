/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

/**
 * K1: [org.jetbrains.kotlin.contracts.description.ContractDescriptionVisitor]
 * K2: [org.jetbrains.kotlin.fir.contracts.description.ConeContractDescriptionVisitor]
 */
public interface KtContractDescriptionVisitor<out R, in D> {
    public fun visitContractDescriptionElement(contractDescriptionElement: KtContractDescriptionElement, data: D): R =
        throw IllegalStateException("Top of hierarchy reached, no overloads were found for element: $contractDescriptionElement")

    // Effects
    public fun visitEffectDeclaration(effectDeclaration: KtEffectDeclaration, data: D): R =
        visitContractDescriptionElement(effectDeclaration, data)

    public fun visitConditionalEffectDeclaration(conditionalEffect: KtConditionalEffectDeclaration, data: D): R =
        visitEffectDeclaration(conditionalEffect, data)

    public fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration, data: D): R =
        visitEffectDeclaration(returnsEffect, data)

    public fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration, data: D): R = visitEffectDeclaration(callsEffect, data)

    // Expressions

    public fun visitBooleanExpression(booleanExpression: KtBooleanExpression, data: D): R = visitContractDescriptionElement(booleanExpression, data)

    public fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: KtBinaryLogicExpression, data: D): R =
        visitBooleanExpression(binaryLogicExpression, data)

    public fun visitLogicalNot(logicalNot: KtLogicalNot, data: D): R = visitBooleanExpression(logicalNot, data)

    public fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate, data: D): R =
        visitBooleanExpression(isInstancePredicate, data)
    public fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate, data: D): R = visitBooleanExpression(isNullPredicate, data)

    // Values
    public fun visitValue(value: KtContractDescriptionValue, data: D): R = visitContractDescriptionElement(value, data)

    public fun visitConstantDescriptor(constantReference: KtConstantReference, data: D): R = visitValue(constantReference, data)
    public fun visitBooleanConstantDescriptor(booleanConstantDescriptor: KtBooleanConstantReference, data: D): R =
        visitConstantDescriptor(booleanConstantDescriptor, data)

    public fun visitValueParameterReference(valueParameterReference: KtValueParameterReference, data: D): R =
        visitValue(valueParameterReference, data)

    public fun visitBooleanValueParameterReference(booleanValueParameterReference: KtBooleanValueParameterReference, data: D): R =
        visitValueParameterReference(booleanValueParameterReference, data)
}
