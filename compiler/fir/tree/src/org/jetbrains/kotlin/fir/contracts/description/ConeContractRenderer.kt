/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.description

import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.renderer.FirRendererComponents
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderForDebugging

class ConeContractRenderer : KtContractDescriptionVisitor<Unit, Nothing?, ConeKotlinType, ConeDiagnostic>() {

    internal lateinit var components: FirRendererComponents
    private val printer get() = components.printer
    private val visitor get() = components.visitor

    fun render(declaration: FirDeclaration) {
        val contractDescription = (declaration as? FirContractDescriptionOwner)?.contractDescription ?: return
        render(contractDescription)
    }

    fun render(contractDescription: FirContractDescription) {
        printer.pushIndent()
        printer.newLine()
        val prefix = when (contractDescription) {
            is FirResolvedContractDescription -> "R|"
            is FirErrorContractDescription -> "E|"
            else -> ""
        }

        printer.print("[${prefix}Contract description]")
        when (contractDescription) {
            is FirLegacyRawContractDescription -> render(contractDescription)
            is FirRawContractDescription -> render(contractDescription)
            is FirResolvedContractDescription -> {
                printer.println()
                render(contractDescription)
            }
            is FirErrorContractDescription -> {}
        }
        printer.popIndent()
    }

    fun render(effectDeclaration: FirEffectDeclaration) {
        printer.newLine()
        printer.println("[Effect declaration] <")
        effectDeclaration.effect.accept(this, null)
        printer.println()
        printer.println(">")
    }

    internal fun render(legacyRawContractDescription: FirLegacyRawContractDescription) {
        printer.renderInBraces("<", ">") {
            legacyRawContractDescription.contractCall.accept(visitor)
            printer.newLine()
        }
    }

    internal fun render(rawContractDescription: FirRawContractDescription) {
        printer.renderInBraces("<", ">") {
            printer.renderSeparatedWithNewlines(rawContractDescription.rawEffects, visitor)
            printer.newLine()
        }
    }

    internal fun render(resolvedContractDescription: FirResolvedContractDescription) {
        printer.println(" <")
        printer.pushIndent()
        resolvedContractDescription.effects.forEach { declaration ->
            declaration.effect.accept(this, null)
            printer.println()
        }
        printer.popIndent()
        printer.println(">")
    }

    override fun visitConditionalEffectDeclaration(conditionalEffect: KtConditionalEffectDeclaration<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        conditionalEffect.effect.accept(this, data)
        printer.print(" -> ")
        conditionalEffect.condition.accept(this, data)
    }

    override fun visitConditionalReturnsDeclaration(
        conditionalEffect: KtConditionalReturnsDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Nothing?,
    ) {
        conditionalEffect.argumentsCondition.accept(this, data)
        printer.print(" -> ")
        conditionalEffect.returnsEffect.accept(this, data)
    }

    override fun visitHoldsInEffectDeclaration(
        holdsInEffect: KtHoldsInEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
        data: Nothing?,
    ) {
        holdsInEffect.argumentsCondition.accept(this, data)
        printer.print(" HoldsIn(")
        holdsInEffect.valueParameterReference.accept(this, data)
        printer.print(")")
    }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        printer.print("Returns(")
        returnsEffect.value.accept(this, data)
        printer.print(")")
    }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        printer.print("CallsInPlace(")
        callsEffect.valueParameterReference.accept(this, data)
        printer.print(", ${callsEffect.kind})")
    }

    override fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.left) { binaryLogicExpression.left.accept(this, data) }
        printer.print(" ${binaryLogicExpression.kind.token} ")
        inBracketsIfNecessary(binaryLogicExpression, binaryLogicExpression.right) { binaryLogicExpression.right.accept(this, data) }
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        inBracketsIfNecessary(logicalNot, logicalNot.arg) { printer.print("!") }
        logicalNot.arg.accept(this, data)
    }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        isInstancePredicate.arg.accept(this, data)
        printer.print(" ${if (isInstancePredicate.isNegated) "!" else ""}is ${isInstancePredicate.type.renderForDebugging()}")
    }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        isNullPredicate.arg.accept(this, data)
        printer.print(" ${if (isNullPredicate.isNegated) "!=" else "=="} null")
    }

    override fun visitConstantDescriptor(constantReference: KtConstantReference<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        printer.print(constantReference.name)
    }

    override fun visitValueParameterReference(valueParameterReference: KtValueParameterReference<ConeKotlinType, ConeDiagnostic>, data: Nothing?) {
        printer.print(valueParameterReference.name)
    }

    private fun inBracketsIfNecessary(parent: KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>, child: KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>, block: () -> Unit) {
        if (needsBrackets(parent, child)) {
            printer.print("(")
            block()
            printer.print(")")
        } else {
            block()
        }
    }

    private fun KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>.isAtom(): Boolean =
        this is KtValueParameterReference || this is KtConstantReference || this is KtIsNullPredicate || this is KtIsInstancePredicate

    private fun needsBrackets(parent: KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>, child: KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>): Boolean {
        if (child.isAtom()) return false
        if (parent is KtLogicalNot) return true
        return parent::class != child::class
    }
}
