/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.fir.renderer.FirRendererComponents
import org.jetbrains.kotlin.fir.types.renderForDebugging

class ConeContractRenderer : ConeContractDescriptionVisitor<Unit, Nothing?>() {

    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer

    override fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: Nothing?) {
        conditionalEffect.effect.accept(this, data)
        printer.print(" -> ")
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Nothing?) {
        printer.print("Returns(")
        returnsEffect.value.accept(this, data)
        printer.print(")")
    }

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Nothing?) {
        printer.print("CallsInPlace(")
        callsEffect.valueParameterReference.accept(this, data)
        printer.print(", ${callsEffect.kind})")
    }

    override fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: ConeBinaryLogicExpression, data: Nothing?) {
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.left) { binaryLogicExpression.left.accept(this, data) }
        printer.print(" ${binaryLogicExpression.kind.token} ")
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.right) { binaryLogicExpression.right.accept(this, data) }
    }

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Nothing?) {
        inBracketsIfNecessary(logicalNot, logicalNot.arg) { printer.print("!") }
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Nothing?) {
        isInstancePredicate.arg.accept(this, data)
        printer.print(" ${if (isInstancePredicate.isNegated) "!" else ""}is ${isInstancePredicate.type.renderForDebugging()}")
    }

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Nothing?) {
        isNullPredicate.arg.accept(this, data)
        printer.print(" ${if (isNullPredicate.isNegated) "!=" else "=="} null")
    }

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Nothing?) {
        printer.print(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Nothing?) {
        printer.print(valueParameterReference.name)
    }

    private fun inBracketsIfNecessary(parent: ConeContractDescriptionElement, child: ConeContractDescriptionElement, block: () -> Unit) {
        if (needsBrackets(parent, child)) {
            printer.print("(")
            block()
            printer.print(")")
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
