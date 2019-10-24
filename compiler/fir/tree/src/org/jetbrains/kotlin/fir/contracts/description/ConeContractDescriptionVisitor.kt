/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description


abstract class ConeContractDescriptionVisitor<out R, in D> {
    open fun visitContractDescriptionElement(contractDescriptionElement: ConeContractDescriptionElement, data: D): R {
        throw IllegalStateException("Top of hierarchy reached, no overloads were found for element: $contractDescriptionElement")
    }

    // Effects
    open fun visitEffectDeclaration(effectDeclaration: ConeEffectDeclaration, data: D): R = visitContractDescriptionElement(effectDeclaration, data)

    open fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: D): R =
        visitEffectDeclaration(conditionalEffect, data)

    open fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: D): R =
        visitEffectDeclaration(returnsEffect, data)

    open fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: D): R =
        visitEffectDeclaration(callsEffect, data)

    // Expressions
    open fun visitBooleanExpression(booleanExpression: ConeBooleanExpression, data: D): R =
        visitContractDescriptionElement(booleanExpression, data)

    open fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: ConeBinaryLogicExpression, data: D): R =
        visitBooleanExpression(binaryLogicExpression, data)

    open fun visitLogicalNot(logicalNot: ConeLogicalNot, data: D): R = visitBooleanExpression(logicalNot, data)

    open fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: D): R =
        visitBooleanExpression(isInstancePredicate, data)

    open fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: D): R = visitBooleanExpression(isNullPredicate, data)

    // Values
    open fun visitValue(value: ConeContractDescriptionValue, data: D): R = visitContractDescriptionElement(value, data)

    open fun visitConstantDescriptor(constantReference: ConeConstantReference, data: D): R = visitValue(constantReference, data)

    open fun visitBooleanConstantDescriptor(booleanConstantDescriptor: ConeBooleanConstantReference, data: D): R =
        visitConstantDescriptor(booleanConstantDescriptor, data)

    open fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: D): R =
        visitValue(valueParameterReference, data)

   open fun visitBooleanValueParameterReference(booleanValueParameterReference: ConeBooleanValueParameterReference, data: D): R =
        visitValueParameterReference(booleanValueParameterReference, data)
}