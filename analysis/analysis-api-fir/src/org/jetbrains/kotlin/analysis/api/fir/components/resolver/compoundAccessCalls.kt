/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.resolutionFacade
import org.jetbrains.kotlin.analysis.api.fir.resolution.*
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Handle compound assignment with array access convention
 */
private fun createKaCallForArrayAccessConvention(
    psi: KtElement,
    fir: FirElement,
    accessExpression: KtExpression?,
    callProvider: (KtElement, FirFunctionCall, KtArrayAccessExpression) -> KaCallResolutionAttempt?,
): KaCallResolutionAttempt? {
    if (fir !is FirFunctionCall || fir.calleeReference.name != OperatorNameConventions.SET || accessExpression !is KtArrayAccessExpression) {
        return null
    }

    return callProvider(psi, fir, accessExpression)
}

/**
 * Handle compound assignment with variable
 */
context(component: KaFirSessionComponent)
private fun createKaCallForVariableAccessConvention(
    psi: KtElement,
    fir: FirElement,
    accessExpression: KtExpression?,
    resolveFragmentOfCall: Boolean,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
    rhsExpression: KtExpression?,
): KaCallResolutionAttempt? {
    if (fir !is FirVariableAssignment || accessExpression !is KtQualifiedExpression && accessExpression !is KtNameReferenceExpression) {
        return null
    }

    val variableSymbol = fir.toPartiallyAppliedSymbol() ?: return null
    val variableAccessCall = KaFirSimpleVariableAccessCall(
        backingPartiallyAppliedSymbol = variableSymbol,
        backingTypeArgumentsMapping = typeArgumentsMapping,
        backingKind = KaFirVariableReadAccess,
        backingIsContextSensitive = fir.calleeReference?.isContextSensitive == true,
    )

    if (resolveFragmentOfCall) {
        return KaFirSimpleCallResolutionSuccess(backingCall = variableAccessCall)
    }

    // Extract operation call
    val firOperationCall = fir.rValue as? FirFunctionCall
        ?: getInitializerOfReferencedLocalVariable(fir.rValue) ?: return null


    val operationError = findErrorCall(firOperationCall, psi)
    val operationAttempt: KaSimpleCallResolutionAttempt = if (operationError != null) {
        operationError
    } else {
        val operationCall = buildOperationCallForCompoundVariableAccess(firOperationCall, accessExpression, rhsExpression)
            ?: return null
        KaFirSimpleCallResolutionSuccess(backingCall = operationCall)
    }

    val variableAttempt = KaFirSimpleCallResolutionSuccess(backingCall = variableAccessCall)
    return KaFirCompoundVariableAccessCallResolutionAttempt(
        backingCompoundOperationProvider = compoundOperationProvider,
        backingVariableCallAttempt = variableAttempt,
        backingOperationCallAttempt = operationAttempt,
    )
}

context(component: KaFirSessionComponent)
private fun createKaCallForCompoundAccessConvention(
    psi: KtElement,
    fir: FirElement,
    accessExpression: KtExpression?,
    rhsExpression: KtExpression?,
    resolveFragmentOfCall: Boolean,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    callProvider: (KtElement, FirFunctionCall, KtArrayAccessExpression) -> KaCallResolutionAttempt?,
    compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
): KaCallResolutionAttempt? = createKaCallForArrayAccessConvention(
    psi = psi,
    fir = fir,
    accessExpression = accessExpression,
    callProvider = callProvider,
) ?: createKaCallForVariableAccessConvention(
    psi = psi,
    fir = fir,
    accessExpression = accessExpression,
    rhsExpression = rhsExpression,
    resolveFragmentOfCall = resolveFragmentOfCall,
    typeArgumentsMapping = typeArgumentsMapping,
    compoundOperationProvider = compoundOperationProvider,
)

context(component: KaFirSessionComponent)
internal fun handleCompoundAccessCall(
    psi: KtElement,
    fir: FirElement,
    resolveFragmentOfCall: Boolean,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
): KaCallResolutionAttempt? {
    return when (psi) {
        is KtBinaryExpression if psi.operationToken in KtTokens.AUGMENTED_ASSIGNMENTS -> {
            val rightOperandPsi = deparenthesize(psi.right) ?: return null
            val leftOperandPsi = deparenthesize(psi.left) ?: return null
            val compoundAssignKind = psi.getCompoundAssignKind()

            createKaCallForCompoundAccessConvention(
                psi = psi,
                fir = fir,
                accessExpression = leftOperandPsi,
                rhsExpression = rightOperandPsi,
                resolveFragmentOfCall = resolveFragmentOfCall,
                typeArgumentsMapping = typeArgumentsMapping,
                callProvider = { psi, fir, arrayAccessExpression ->
                    createCompoundArrayAccessCall(
                        psi = psi,
                        firCall = fir,
                        lhsArrayAccessExpression = arrayAccessExpression,
                        rhsExpression = rightOperandPsi,
                        resolveFragmentOfCall = resolveFragmentOfCall,
                        compoundOperationProvider = { KaFirCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
                    )
                },
                compoundOperationProvider = { KaFirCompoundAssignOperation(it, compoundAssignKind, rightOperandPsi) },
            )
        }

        is KtUnaryExpression if psi.operationToken in KtTokens.INCREMENT_AND_DECREMENT -> {
            val precedence = when (psi) {
                is KtPostfixExpression -> KaCompoundUnaryOperation.Precedence.POSTFIX
                else -> KaCompoundUnaryOperation.Precedence.PREFIX
            }

            val incOrDecOperationKind = psi.getInOrDecOperationKind()
            val baseExpression = deparenthesize(psi.baseExpression)

            createKaCallForCompoundAccessConvention(
                psi = psi,
                fir = fir,
                accessExpression = baseExpression,
                rhsExpression = null,
                resolveFragmentOfCall = resolveFragmentOfCall,
                typeArgumentsMapping = typeArgumentsMapping,
                callProvider = { psi, firCall, ktExpression ->
                    createCompoundArrayAccessCall(
                        psi = psi,
                        firCall = firCall,
                        lhsArrayAccessExpression = ktExpression,
                        rhsExpression = null,
                        resolveFragmentOfCall = resolveFragmentOfCall,
                        compoundOperationProvider = { KaFirCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
                    )
                },
                compoundOperationProvider = { KaFirCompoundUnaryOperation(it, incOrDecOperationKind, precedence) },
            )
        }
        else -> null
    }
}

context(component: KaFirSessionComponent)
private fun createCompoundArrayAccessCall(
    psi: KtElement,
    firCall: FirFunctionCall,
    lhsArrayAccessExpression: KtArrayAccessExpression,
    rhsExpression: KtExpression?,
    resolveFragmentOfCall: Boolean,
    compoundOperationProvider: (KaFunctionCall<KaNamedFunctionSymbol>) -> KaCompoundOperation,
): KaCallResolutionAttempt? {
    // The last argument of `set` is the new value to be set. This value should be a call to the respective `plus`, `minus`,
    // `times`, `div`, or `rem` function.
    val firOperationCall = firCall.arguments.lastOrNull() as? FirFunctionCall ?: return null

    // The explicit receiver for both `get` and `set` call should be the array expression.
    val firExplicitReceiver = firOperationCall.explicitReceiver ?: return null
    val firGetCall = firExplicitReceiver as? FirFunctionCall // case for array access and prefix
        ?: getInitializerOfReferencedLocalVariable(firExplicitReceiver) // case for postfix
        ?: return null

    val indexExpressions = lhsArrayAccessExpression.indexExpressions
    val arrayExpression = lhsArrayAccessExpression.arrayExpression

    // Build getter call or error
    val getterError = findErrorCall(firGetCall, psi)
    val getterAttempt: KaSimpleCallResolutionAttempt
    if (getterError != null) {
        getterAttempt = getterError
    } else {
        val getPartiallyAppliedSymbol = firGetCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
        val getArgumentMapping = indexExpressions.zip(getPartiallyAppliedSymbol.signature.valueParameters).toMap()
        val getCall = KaFirSimpleFunctionCall(
            backingPartiallyAppliedSymbol = getPartiallyAppliedSymbol,
            backingArgumentMapping = getArgumentMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = getPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        ).let {
            @Suppress("UNCHECKED_CAST")
            it as KaFunctionCall<KaNamedFunctionSymbol>
        }

        getterAttempt = KaFirSimpleCallResolutionSuccess(backingCall = getCall)
    }

    if (resolveFragmentOfCall) {
        return getterAttempt
    }

    // Build operation call or error
    val operationError = findErrorCall(firOperationCall, psi)
    val operationAttempt: KaSimpleCallResolutionAttempt = if (operationError != null) {
        operationError
    } else {
        // The explicit receiver in this case is a synthetic FirFunctionCall to `get`, which does not have a corresponding PSI. So
        // we use the `lhsArrayAccessExpression` as the supplement.
        val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(lhsArrayAccessExpression) ?: return null
        val operationArgumentsMapping = listOfNotNull(lhsArrayAccessExpression, rhsExpression)
            .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
            .toMap()
        val operationCall = KaFirSimpleFunctionCall(
            backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
            backingArgumentMapping = operationArgumentsMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        ).let {
            @Suppress("UNCHECKED_CAST")
            it as KaFunctionCall<KaNamedFunctionSymbol>
        }

        KaFirSimpleCallResolutionSuccess(backingCall = operationCall)
    }

    // Build setter call or error
    val setterError = findErrorCall(firCall, psi)
    val setterAttempt: KaSimpleCallResolutionAttempt
    if (setterError != null) {
        setterAttempt = setterError
    } else {
        val setPartiallyAppliedSymbol = firCall.toPartiallyAppliedSymbol(arrayExpression) ?: return null
        val setArgumentsMapping = mapOf(indexExpressions.last() to setPartiallyAppliedSymbol.signature.valueParameters.last())
        val setCall = KaFirSimpleFunctionCall(
            backingPartiallyAppliedSymbol = setPartiallyAppliedSymbol,
            backingArgumentMapping = setArgumentsMapping,
            backingTypeArgumentsMapping = firCall
                .toFirTypeArgumentsMapping(symbol = setPartiallyAppliedSymbol.symbol.firSymbol)
                .asKaTypeParametersMapping(),
        ).let {
            @Suppress("UNCHECKED_CAST")
            it as KaFunctionCall<KaNamedFunctionSymbol>
        }

        setterAttempt = KaFirSimpleCallResolutionSuccess(backingCall = setCall)
    }

    return KaFirCompoundArrayAccessCallResolutionAttempt(
        backingCompoundOperationProvider = compoundOperationProvider,
        backingIndexArguments = indexExpressions,
        backingGetterCallAttempt = getterAttempt,
        backingOperationCallAttempt = operationAttempt,
        backingSetterCallAttempt = setterAttempt,
    )
}

context(component: KaFirSessionComponent)
private fun getInitializerOfReferencedLocalVariable(variableReference: FirExpression): FirFunctionCall? {
    return variableReference.toReference(resolutionFacade.useSiteFirSession)
        ?.toResolvedVariableSymbol()
        ?.fir
        ?.initializer as? FirFunctionCall
}

context(component: KaFirSessionComponent)
private fun buildOperationCallForCompoundVariableAccess(
    firOperationCall: FirFunctionCall,
    leftOperandPsi: KtExpression,
    rightOperandPsi: KtExpression?,
): KaFunctionCall<KaNamedFunctionSymbol>? {
    val operationPartiallyAppliedSymbol = firOperationCall.toPartiallyAppliedSymbol(leftOperandPsi) ?: return null

    val operationArgumentsMapping = listOfNotNull(leftOperandPsi, rightOperandPsi)
        .zip(operationPartiallyAppliedSymbol.signature.valueParameters)
        .toMap()

    val functionCall = KaFirSimpleFunctionCall(
        backingPartiallyAppliedSymbol = operationPartiallyAppliedSymbol,
        backingArgumentMapping = operationArgumentsMapping,
        backingTypeArgumentsMapping = firOperationCall
            .toFirTypeArgumentsMapping(symbol = operationPartiallyAppliedSymbol.symbol.firSymbol)
            .asKaTypeParametersMapping(),
    )

    @Suppress("UNCHECKED_CAST")
    return functionCall as KaFunctionCall<KaNamedFunctionSymbol>
}

private fun KtBinaryExpression.getCompoundAssignKind(): KaCompoundAssignOperation.Kind = when (operationToken) {
    KtTokens.PLUSEQ -> KaCompoundAssignOperation.Kind.PLUS_ASSIGN
    KtTokens.MINUSEQ -> KaCompoundAssignOperation.Kind.MINUS_ASSIGN
    KtTokens.MULTEQ -> KaCompoundAssignOperation.Kind.TIMES_ASSIGN
    KtTokens.PERCEQ -> KaCompoundAssignOperation.Kind.REM_ASSIGN
    KtTokens.DIVEQ -> KaCompoundAssignOperation.Kind.DIV_ASSIGN
    else -> error("unexpected operator $operationToken")
}

private fun KtUnaryExpression.getInOrDecOperationKind(): KaCompoundUnaryOperation.Kind = when (operationToken) {
    KtTokens.PLUSPLUS -> KaCompoundUnaryOperation.Kind.INC
    KtTokens.MINUSMINUS -> KaCompoundUnaryOperation.Kind.DEC
    else -> error("unexpected operator $operationToken")
}
