/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description


abstract class KtContractDescriptionVisitor<out R, in D, Type, Diagnostic> {
    open fun visitContractDescriptionElement(contractDescriptionElement: KtContractDescriptionElement<Type, Diagnostic>, data: D): R {
        throw IllegalStateException("Top of hierarchy reached, no overloads were found for element: $contractDescriptionElement")
    }

    // Effects
    open fun visitEffectDeclaration(effectDeclaration: KtEffectDeclaration<Type, Diagnostic>, data: D): R = visitContractDescriptionElement(effectDeclaration, data)

    open fun visitConditionalEffectDeclaration(conditionalEffect: KtConditionalEffectDeclaration<Type, Diagnostic>, data: D): R =
        visitEffectDeclaration(conditionalEffect, data)

    open fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<Type, Diagnostic>, data: D): R =
        visitEffectDeclaration(returnsEffect, data)

    open fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<Type, Diagnostic>, data: D): R =
        visitEffectDeclaration(callsEffect, data)

    open fun visitErroneousCallsEffectDeclaration(callsEffect: KtErroneousCallsEffectDeclaration<Type, Diagnostic>, data: D): R =
        visitCallsEffectDeclaration(callsEffect, data)

    // Expressions
    open fun visitBooleanExpression(booleanExpression: KtBooleanExpression<Type, Diagnostic>, data: D): R =
        visitContractDescriptionElement(booleanExpression, data)

    open fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: KtBinaryLogicExpression<Type, Diagnostic>, data: D): R =
        visitBooleanExpression(binaryLogicExpression, data)

    open fun visitLogicalNot(logicalNot: KtLogicalNot<Type, Diagnostic>, data: D): R = visitBooleanExpression(logicalNot, data)

    open fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<Type, Diagnostic>, data: D): R =
        visitBooleanExpression(isInstancePredicate, data)

    open fun visitErroneousIsInstancePredicate(isInstancePredicate: KtErroneousIsInstancePredicate<Type, Diagnostic>, data: D): R =
        visitIsInstancePredicate(isInstancePredicate, data)

    open fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<Type, Diagnostic>, data: D): R = visitBooleanExpression(isNullPredicate, data)

    // Values
    open fun visitValue(value: KtContractDescriptionValue<Type, Diagnostic>, data: D): R = visitContractDescriptionElement(value, data)

    open fun visitConstantDescriptor(constantReference: KtConstantReference<Type, Diagnostic>, data: D): R = visitValue(constantReference, data)

    open fun visitBooleanConstantDescriptor(booleanConstantDescriptor: KtBooleanConstantReference<Type, Diagnostic>, data: D): R =
        visitConstantDescriptor(booleanConstantDescriptor, data)

    open fun visitErroneousConstantReference(erroneousConstantReference: KtErroneousConstantReference<Type, Diagnostic>, data: D): R =
        visitConstantDescriptor(erroneousConstantReference, data)

    open fun visitValueParameterReference(valueParameterReference: KtValueParameterReference<Type, Diagnostic>, data: D): R =
        visitValue(valueParameterReference, data)

    open fun visitBooleanValueParameterReference(booleanValueParameterReference: KtBooleanValueParameterReference<Type, Diagnostic>, data: D): R =
        visitValueParameterReference(booleanValueParameterReference, data)

    open fun visitErroneousValueParameterReference(valueParameterReference: KtErroneousValueParameterReference<Type, Diagnostic>, data: D): R =
        visitValueParameterReference(valueParameterReference, data)

    // Error
    open fun visitErroneousElement(element: KtErroneousContractElement<Type, Diagnostic>, data: D): R =
        visitContractDescriptionElement(element, data)
}
