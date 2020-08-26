/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference

class ConeContractRenderer(private val builder: StringBuilder) : ConeContractDescriptionVisitor<Unit, Nothing?>() {
    override fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: Nothing?) {
        conditionalEffect.effect.accept(this, data)
        builder.append(" -> ")
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Nothing?) {
        builder.append("Returns(")
        returnsEffect.value.accept(this, data)
        builder.append(")")
    }

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Nothing?) {
        builder.append("CallsInPlace(")
        callsEffect.valueParameterReference.accept(this, data)
        builder.append(", ${callsEffect.kind})")
    }

    override fun visitThrowsEffectDeclaration(throwsEffect: ConeThrowsEffectDeclaration, data: Nothing?) {
        builder.append("throws ").append(throwsEffect.exceptionType)
    }

    override fun visitCalledInTryCatchEffectDeclaration(calledInEffect: ConeCalledInTryCatchEffectDeclaration, data: Nothing?) {
        builder.append("calledInTryCatch<${calledInEffect.exceptionType}>(")
        calledInEffect.lambda.accept(this, data)
        builder.append(")")
    }

    override fun visitMustDoEffectDeclaration(mustDoEffect: ConeMustDoEffectDeclaration, data: Nothing?) {
        mustDoEffect.lambda.accept(this, data)
        builder.append(" mustDo ")
        mustDoEffect.action.accept(this, data)
    }

    override fun visitProvidesActionEffectDeclaration(providesActionEffect: ConeProvidesActionEffectDeclaration, data: Nothing?) {
        builder.append("provides ")
        providesActionEffect.action.accept(this, data)
    }

    override fun visitRequiresActionEffectDeclaration(requiresActionEffect: ConeRequiresActionEffectDeclaration, data: Nothing?) {
        builder.append("requires ")
        requiresActionEffect.action.accept(this, data)
    }

    override fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: ConeBinaryLogicExpression, data: Nothing?) {
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.left) { binaryLogicExpression.left.accept(this, data) }
        builder.append(" ${binaryLogicExpression.kind.token} ")
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.right) { binaryLogicExpression.right.accept(this, data) }
    }

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Nothing?) {
        inBracketsIfNecessary(logicalNot, logicalNot.arg) { builder.append("!") }
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Nothing?) {
        isInstancePredicate.arg.accept(this, data)
        builder.append(" ${if (isInstancePredicate.isNegated) "!" else ""}is ${isInstancePredicate.type}")
    }

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Nothing?) {
        isNullPredicate.arg.accept(this, data)
        builder.append(" ${if (isNullPredicate.isNegated) "!=" else "=="} null")
    }

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Nothing?) {
        builder.append(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Nothing?) {
        builder.append(valueParameterReference.name)
    }

    override fun visitLambdaArgumentReference(lambdaArgumentReference: ConeLambdaArgumentReference, data: Nothing?) {
        lambdaArgumentReference.parameter.accept(this, data)
        builder.append("@")
        lambdaArgumentReference.lambda.accept(this, data)
    }

    override fun visitPropertyInitializationAction(propertyInitializationAction: ConePropertyInitializationAction, data: Nothing?) {
        builder.append("initializationOf(")
        propertyInitializationAction.target.accept(this, data)
        builder.append(", ", propertyInitializationAction.property.callableId.callableName)
        builder.append(", ${propertyInitializationAction.kind})")
    }

    override fun visitFunctionInvocationAction(functionInvocationAction: ConeFunctionInvocationAction, data: Nothing?) {
        builder.append("invocationOf(")
        functionInvocationAction.target.accept(this, data)
        builder.append(", ", functionInvocationAction.function.callableId.callableName)
        builder.append(", ${functionInvocationAction.kind})")
    }

    private fun inBracketsIfNecessary(parent: ConeContractDescriptionElement, child: ConeContractDescriptionElement, block: () -> Unit) {
        if (needsBrackets(parent, child)) {
            builder.append("(")
            block()
            builder.append(")")
        } else {
            block()
        }
    }

    private fun ConeContractDescriptionElement.isAtom(): Boolean =
        this is VariableReference || this is ConstantReference || this is ConeIsNullPredicate || this is ConeIsInstancePredicate

    private fun needsBrackets(parent: ConeContractDescriptionElement, child: ConeContractDescriptionElement): Boolean {
        if (child.isAtom()) return false
        if (parent is ConeLogicalNot) return true
        return parent::class != child::class
    }
}