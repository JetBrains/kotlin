/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.contracts.description.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isCastErased
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeContractMayNotHaveLabel
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeContractDescriptionError
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

object FirContractChecker : FirFunctionChecker(MppCheckerKind.Common) {
    private const val EMPTY_CONTRACT_MESSAGE = "Empty contract block is not allowed"
    private const val DUPLICATE_CALLS_IN_PLACE_MESSAGE = "A value parameter may not be annotated with callsInPlace twice"
    private const val INVALID_CONTRACT_BLOCK = "Contract block could not be resolved"
    private const val CALLS_IN_PLACE_ON_CONTEXT_PARAMETER =
        "callsInPlace contract cannot be applied to context parameter because context arguments can never be lambdas."

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (declaration !is FirContractDescriptionOwner) return
        val contractDescription = declaration.contractDescription ?: return

        // For K1 compatibility, we do not check the contract description if the contract is in a place where contracts aren't allowed.
        // TODO: (KT-72772) Decide whether some errors should be emitted even for not allowed contracts.
        val reportedNotAllowed = checkContractNotAllowed(declaration, contractDescription, context, reporter)
        if (reportedNotAllowed) return

        val contractCall = (declaration.body?.statements?.firstOrNull() as? FirContractCallBlock)?.call
        if (contractCall != null) {
            checkAnnotationsNotAllowed(contractCall, context, reporter)
        }

        when (contractDescription) {
            is FirResolvedContractDescription -> {
                checkUnresolvedEffects(contractDescription, declaration, context, reporter)
                checkDuplicateCallsInPlace(contractDescription, context, reporter)
                if (declaration.contextParameters.isNotEmpty()) {
                    checkCallsInPlaceOnContextParameter(contractDescription, declaration.valueParameters.size, context, reporter)
                }
                if (contractDescription.effects.isEmpty() && contractDescription.unresolvedEffects.isEmpty()) {
                    reporter.reportOn(contractDescription.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, EMPTY_CONTRACT_MESSAGE)
                }
                checkDiagnosticsFromFirBuilder(contractDescription.diagnostic, contractDescription.source, context, reporter)
            }
            is FirErrorContractDescription -> {
                reporter.reportOn(contractDescription.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, INVALID_CONTRACT_BLOCK)
                checkDiagnosticsFromFirBuilder(contractDescription.diagnostic, contractDescription.source, context, reporter)
            }
            is FirRawContractDescription, is FirLegacyRawContractDescription ->
                errorWithAttachment("Unexpected contract description kind: ${contractDescription::class.simpleName}") {
                    withFirEntry("declaration", declaration)
                }
        }
    }

    private fun checkAnnotationsNotAllowed(
        contractCall: FirFunctionCall,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val argument = contractCall.arguments.singleOrNull() as? FirAnonymousFunctionExpression ?: return
        if (!argument.anonymousFunction.isLambda) return
        val lambdaBody = argument.anonymousFunction.body ?: return

        lambdaBody.acceptChildren(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnnotation(annotation: FirAnnotation) {
                reporter.reportOn(annotation.source, FirErrors.ANNOTATION_IN_CONTRACT_ERROR, context)
            }

            override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
                reporter.reportOn(annotationCall.source, FirErrors.ANNOTATION_IN_CONTRACT_ERROR, context)
            }
        })
    }

    private fun checkUnresolvedEffects(
        contractDescription: FirResolvedContractDescription,
        declaration: FirFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val erasedCastChecker =
            if (context.languageVersionSettings.supportsFeature(LanguageFeature.AllowCheckForErasedTypesInContracts)) null
            else ErasedCastChecker(declaration, context)
        // Any statements that [ConeEffectExtractor] cannot extract effects will be in `unresolvedEffects`.
        for (unresolvedEffect in contractDescription.unresolvedEffects) {
            // We only check for erased casts if we cannot find an existing diagnostic, since they will sometimes be caught by the
            // cone effect extractor already.
            val diagnostic =
                unresolvedEffect.effect.accept(DiagnosticExtractor, null)
                    ?: erasedCastChecker?.let { unresolvedEffect.effect.accept(it, null) }
                    ?: continue

            // TODO, KT-59806: report on fine-grained locations, e.g., ... implies unresolved => report on unresolved, not the entire statement.
            //  but, sometimes, it's just reported on `contract`...
            reporter.reportOn(unresolvedEffect.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, diagnostic.reason, context)
        }

        if (erasedCastChecker != null) {
            for (resolvedEffect in contractDescription.effects) {
                val diagnostic = resolvedEffect.effect.accept(erasedCastChecker, null) ?: continue
                reporter.reportOn(resolvedEffect.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, diagnostic.reason, context)
            }
        }
    }

    private fun checkContractNotAllowed(
        declaration: FirFunction,
        contractDescription: FirContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ): Boolean {
        val source = contractDescription.source
        if (source?.kind !is KtRealSourceElementKind) return false

        fun contractNotAllowed(message: String) = reporter.reportOn(source, FirErrors.CONTRACT_NOT_ALLOWED, message, context)

        when {
            declaration is FirPropertyAccessor || declaration is FirAnonymousFunction -> {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.AllowContractsOnPropertyAccessors)) {
                    if (declaration is FirAnonymousFunction) contractNotAllowed("Contracts are not allowed for anonymous functions.")
                } else {
                    contractNotAllowed("Contracts are only allowed for functions.")
                }
            }
            declaration.isAbstract || declaration.isOpen || declaration.isOverride -> {
                contractNotAllowed("Contracts are not allowed for open or override functions.")
            }
            declaration.isOperator -> {
                if (context.languageVersionSettings.supportsFeature(LanguageFeature.AllowContractsOnSomeOperators)) {
                    if (declaration.isContractOnOperatorForbidden())
                        contractNotAllowed("Contracts are not allowed for operator ${declaration.nameOrSpecialName}.")
                } else {
                    contractNotAllowed("Contracts are not allowed for operator functions.")
                }
            }
            declaration.symbol.callableId.isLocal || declaration.visibility == Visibilities.Local -> {
                contractNotAllowed("Contracts are not allowed for local functions.")
            }
            else -> return false
        }
        return true
    }

    private fun FirFunction.isContractOnOperatorForbidden(): Boolean = when (nameOrSpecialName) {
        // according to KT-73742, KT-73313 and discussions linked to them
        OperatorNameConventions.EQUALS,
        OperatorNameConventions.COMPARE_TO,
        OperatorNameConventions.GET_VALUE,
        OperatorNameConventions.SET_VALUE,
        OperatorNameConventions.PROVIDE_DELEGATE,
            -> true
        // Operators related to augmented assignment desugaring
        // TODO: enable in the future (KT-77175)
        OperatorNameConventions.GET,
        OperatorNameConventions.SET,
        OperatorNameConventions.PLUS,
        OperatorNameConventions.MINUS,
        OperatorNameConventions.TIMES,
        OperatorNameConventions.DIV,
        OperatorNameConventions.REM,
        OperatorNameConventions.PLUS_ASSIGN,
        OperatorNameConventions.MINUS_ASSIGN,
        OperatorNameConventions.TIMES_ASSIGN,
        OperatorNameConventions.DIV_ASSIGN,
        OperatorNameConventions.REM_ASSIGN,
            -> true
        else -> false
    }

    private fun checkDuplicateCallsInPlace(
        description: FirResolvedContractDescription,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val callsInPlaceEffects = description.effects.mapNotNull { it.effect as? ConeCallsEffectDeclaration }
        val seenParameterIndices = mutableSetOf<Int>()

        for (effect in callsInPlaceEffects) {
            val parameterIndex = effect.valueParameterReference.parameterIndex
            if (parameterIndex in seenParameterIndices) {
                reporter.reportOn(description.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, DUPLICATE_CALLS_IN_PLACE_MESSAGE, context)
            } else {
                seenParameterIndices.add(parameterIndex)
            }
        }
    }

    private fun checkCallsInPlaceOnContextParameter(
        description: FirResolvedContractDescription,
        valueParametersCount: Int,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (effectDeclaration in description.effects) {
            val effect = effectDeclaration.effect
            if (effect !is ConeCallsEffectDeclaration) continue
            if (effect.valueParameterReference.parameterIndex >= valueParametersCount) {
                reporter.reportOn(description.source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, CALLS_IN_PLACE_ON_CONTEXT_PARAMETER, context)
            }
        }
    }

    private fun checkDiagnosticsFromFirBuilder(
        diagnostic: ConeDiagnostic?,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        when (diagnostic) {
            ConeContractMayNotHaveLabel ->
                reporter.reportOn(source, FirErrors.ERROR_IN_CONTRACT_DESCRIPTION, ConeContractMayNotHaveLabel.reason, context)
        }
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

        override fun visitErroneousElement(
            element: KtErroneousContractElement<ConeKotlinType, ConeDiagnostic>, data: Nothing?
        ): ConeDiagnostic {
            return element.diagnostic
        }
    }

    private class ErasedCastChecker(val declaration: FirFunction, val context: CheckerContext) :
        KtContractDescriptionVisitor<ConeDiagnostic?, Nothing?, ConeKotlinType, ConeDiagnostic>() {
        override fun visitContractDescriptionElement(
            contractDescriptionElement: KtContractDescriptionElement<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic? {
            return null
        }

        override fun visitConditionalEffectDeclaration(
            conditionalEffect: KtConditionalEffectDeclaration<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic? {
            return conditionalEffect.condition.accept(this, data)
        }

        override fun visitConditionalReturnsDeclaration(
            conditionalEffect: KtConditionalReturnsDeclaration<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?,
        ): ConeDiagnostic? {
            return conditionalEffect.argumentsCondition.accept(this, data)
        }

        override fun visitIsInstancePredicate(
            isInstancePredicate: KtIsInstancePredicate<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic? {
            val parameterType = getParameterType(isInstancePredicate.arg.parameterIndex)
            return isCastErased(parameterType, isInstancePredicate.type, context).ifTrue {
                ConeContractDescriptionError.ErasedIsCheck
            }
        }

        override fun visitLogicalBinaryOperationContractExpression(
            binaryLogicExpression: KtBinaryLogicExpression<ConeKotlinType, ConeDiagnostic>,
            data: Nothing?
        ): ConeDiagnostic? {
            return binaryLogicExpression.left.accept(this, data) ?: binaryLogicExpression.right.accept(this, data)
        }

        override fun visitLogicalNot(logicalNot: KtLogicalNot<ConeKotlinType, ConeDiagnostic>, data: Nothing?): ConeDiagnostic? {
            return logicalNot.arg.accept(this, data)
        }

        private fun getParameterType(index: Int): ConeKotlinType =
            when (index) {
                -1 -> declaration.symbol.resolvedReceiverType
                    ?: declaration.symbol.dispatchReceiverType
                    ?: error("Contract references non-existent receiver")
                in declaration.valueParameters.indices -> declaration.valueParameters[index].returnTypeRef.coneType
                else -> declaration.contextParameters[index - declaration.valueParameters.size].returnTypeRef.coneType
            }
    }
}
