/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

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
        this is ConeValueParameterReference || this is ConeConstantReference || this is ConeIsNullPredicate || this is ConeIsInstancePredicate

    private fun needsBrackets(parent: ConeContractDescriptionElement, child: ConeContractDescriptionElement): Boolean {
        if (child.isAtom()) return false
        if (parent is ConeLogicalNot) return true
        return parent::class != child::class
    }
}
