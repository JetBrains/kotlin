/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object FirContractChecker : FirFunctionChecker() {
    private val EMPTY_CONTRACT_MESSAGE = "Empty contract block is not allowed"

    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirContractDescriptionOwner) return
        val contractDescription = declaration.contractDescription as? FirResolvedContractDescription ?: return

        checkUnresolvedEffects(contractDescription, context, reporter)
        checkContractNotAllowed(declaration, contractDescription, context, reporter)
        if (contractDescription.effects.isEmpty() && contractDescription.unresolvedEffects.isEmpty()) {
            reporter.reportOn(contractDescription.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, EMPTY_CONTRACT_MESSAGE, context)
        }
    }

    private fun checkUnresolvedEffects(
        contractDescription: FirResolvedContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        // Any statements that [ConeEffectExtractor] cannot extract effects will be in `unresolvedEffects`.
        for (unresolvedEffect in contractDescription.unresolvedEffects) {
            val diagnostic = unresolvedEffect.effect.accept(DiagnosticExtractor, null) ?: continue

            // TODO, KT-59806: report on fine-grained locations, e.g., ... implies unresolved => report on unresolved, not the entire statement.
            //  but, sometimes, it's just reported on `contract`...
            reporter.reportOn(unresolvedEffect.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, diagnostic.reason, context)
        }
    }

    private fun checkContractNotAllowed(
        declaration: FirFunction,
        contractDescription: FirResolvedContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val source = contractDescription.source
        if (source?.kind !is KtRealSourceElementKind) return

        fun contractNotAllowed(message: String) = reporter.reportOn(source, FirErrors.CONTRACT_NOT_ALLOWED, message, context)

        if (declaration is FirPropertyAccessor || declaration is FirAnonymousFunction) contractNotAllowed("Contracts are only allowed for functions.")
        else if (declaration.isAbstract || declaration.isOpen || declaration.isOverride) contractNotAllowed("Contracts are not allowed for open or override functions.")
        else if (declaration.isOperator) contractNotAllowed("Contracts are not allowed for operator functions.")
        else if (declaration.symbol.callableId.isLocal || declaration.visibility == Visibilities.Local) contractNotAllowed("Contracts are not allowed for local functions.")
    }

    private object DiagnosticExtractor : KtContractDescriptionVisitor<ConeDiagnostic?, Nothing?, ConeKotlinType, ConeDiagnostic>() {
        override fun visitContractDescriptionElement(
            contractDescriptionElement: ConeContractDescriptionElement,
            data: Nothing?
        ): ConeDiagnostic? {
            return null
        }

        override fun visitConditionalEffectDeclaration(
            conditionalEffect: ConeConditionalEffectDeclaration,
            data: Nothing?
        ): ConeDiagnostic? {
            return conditionalEffect.effect.accept(this, null) ?: conditionalEffect.condition.accept(this, null)
        }

        override fun visitReturnsEffectDeclaration(returnsEffect: ConeReturnsEffectDeclaration, data: Nothing?): ConeDiagnostic? {
            return returnsEffect.value.accept(this, null)
        }

        override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Nothing?): ConeDiagnostic? {
            return callsEffect.valueParameterReference.accept(this, data)
        }

        override fun visitErroneousCallsEffectDeclaration(
            callsEffect: KtErroneousCallsEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic {
            return callsEffect.diagnostic
        }

        override fun visitLogicalBinaryOperationContractExpression(
            binaryLogicExpression: ConeBinaryLogicExpression,
            data: Nothing?
        ): ConeDiagnostic? {
            return binaryLogicExpression.left.accept(this, null) ?: binaryLogicExpression.right.accept(this, null)
        }

        override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Nothing?): ConeDiagnostic? {
            return logicalNot.arg.accept(this, null)
        }

        override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Nothing?): ConeDiagnostic? {
            return isInstancePredicate.arg.accept(this, data)
        }

        override fun visitErroneousIsInstancePredicate(
            isInstancePredicate: KtErroneousIsInstancePredicate<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic {
            return isInstancePredicate.diagnostic
        }

        override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Nothing?): ConeDiagnostic? {
            return isNullPredicate.arg.accept(this, data)
        }

        override fun visitErroneousConstantReference(
            erroneousConstantReference: KtErroneousConstantReference<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic {
            return erroneousConstantReference.diagnostic
        }

        override fun visitErroneousValueParameterReference(
            valueParameterReference: KtErroneousValueParameterReference<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic {
            return valueParameterReference.diagnostic
        }

        override fun visitErroneousElement(element: KtErroneousContractElement<ConeKotlinType, ConeDiagnostic>, data: Nothing?): ConeDiagnostic {
            return element.diagnostic
        }
    }
}
