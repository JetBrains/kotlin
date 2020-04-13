/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.inferenceContext
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.preprocessCallableReference
import org.jetbrains.kotlin.fir.resolve.inference.preprocessLambdaArgument
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.scopes.impl.FirILTTypeRefPlaceHolder
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


fun Candidate.resolveArgumentExpression(
    /*
    csBuilder: ConstraintSystemBuilder,
    argument: KotlinCallArgument,
    expectedType: UnwrappedType?,
    diagnosticsHolder: KotlinDiagnosticsHolder,
    isReceiver: Boolean
     */
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType,
    expectedTypeRef: FirTypeRef,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    isSafeCall: Boolean
) {
    when (argument) {
        is FirFunctionCall, is FirWhenExpression, is FirTryExpression, is FirCheckNotNullCall -> resolveSubCallArgument(
            csBuilder,
            argument as FirResolvable,
            expectedType,
            sink,
            isReceiver,
            isDispatch,
            isSafeCall
        )
        is FirCallableReferenceAccess ->
            if (argument.calleeReference is FirResolvedNamedReference)
                resolvePlainExpressionArgument(
                    csBuilder,
                    argument,
                    expectedType,
                    sink,
                    isReceiver,
                    isDispatch,
                    isSafeCall
                )
            else
                preprocessCallableReference(argument, expectedType)
        // NB: FirCallableReferenceAccess should be checked earlier
        is FirQualifiedAccessExpression -> resolvePlainExpressionArgument(
            csBuilder,
            argument,
            expectedType,
            sink,
            isReceiver,
            isDispatch,
            isSafeCall
        )
        // TODO:!
        is FirAnonymousFunction -> preprocessLambdaArgument(csBuilder, argument, expectedType, expectedTypeRef)
        // TODO:!
        //TODO: Collection literal
        is FirWrappedArgumentExpression -> resolveArgumentExpression(
            csBuilder,
            argument.expression,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            isDispatch,
            isSafeCall
        )
        is FirBlock -> resolveBlockArgument(
            csBuilder,
            argument,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            isDispatch,
            isSafeCall
        )
        else -> resolvePlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, isDispatch, isSafeCall)
    }
}

private fun Candidate.resolveBlockArgument(
    csBuilder: ConstraintSystemBuilder,
    block: FirBlock,
    expectedType: ConeKotlinType,
    expectedTypeRef: FirTypeRef,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    isSafeCall: Boolean
) {
    val returnArguments = block.returnExpressions()
    if (returnArguments.isEmpty()) {
        checkApplicabilityForArgumentType(
            csBuilder,
            block.typeRef.coneTypeUnsafe(),
            expectedType.type,
            SimpleConstraintSystemConstraintPosition,
            isReceiver = false,
            isDispatch = false,
            nullableExpectedType = expectedType.type.withNullability(ConeNullability.NULLABLE, sink.components.session.inferenceContext),
            sink = sink
        )
        return
    }
    for (argument in returnArguments) {
        resolveArgumentExpression(
            csBuilder,
            argument,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            isDispatch,
            isSafeCall
        )
    }
}

fun Candidate.resolveSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirResolvable,
    expectedType: ConeKotlinType,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    isSafeCall: Boolean
) {
    val candidate = argument.candidate() ?: return resolvePlainExpressionArgument(
        csBuilder,
        argument as FirExpression,
        expectedType,
        sink,
        isReceiver,
        isDispatch,
        isSafeCall
    )
    /*
     * It's important to extract type from argument neither from symbol, because of symbol contains
     *   placeholder type with value 0, but argument contains type with proper literal value
     */
    val type: ConeKotlinType = if (candidate.symbol.fir is FirIntegerOperator) {
        (argument as FirFunctionCall).resultType.coneTypeUnsafe()
    } else {
        sink.components.returnTypeCalculator.tryCalculateReturnType(candidate.symbol.firUnsafe()).coneTypeUnsafe()
    }
    val argumentType = candidate.substitutor.substituteOrSelf(type)
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isDispatch, isSafeCall)
}

fun Candidate.resolvePlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    isSafeCall: Boolean
) {
    if (expectedType == null) return
    val argumentType = argument.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isDispatch, isSafeCall)
    checkApplicabilityForIntegerOperatorCall(sink, argument)
}

private fun Candidate.checkApplicabilityForIntegerOperatorCall(sink: CheckerSink, argument: FirExpression) {
    if (symbol.fir !is FirIntegerOperator) return
    if (argument !is FirConstExpression<*> && argument !is FirIntegerOperatorCall) {
        sink.reportApplicability(CandidateApplicability.INAPPLICABLE)
    }
}

fun Candidate.resolvePlainArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    isSafeCall: Boolean
) {
    val position = SimpleConstraintSystemConstraintPosition //TODO

    val session = sink.components.session
    val capturedType = prepareCapturedType(argumentType)

    val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, session.inferenceContext)
    if (isReceiver && isSafeCall) {
        if (!isDispatch && !csBuilder.addSubtypeConstraintIfCompatible(capturedType, nullableExpectedType, position)) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER) // TODO
        }
        return
    }

    checkApplicabilityForArgumentType(csBuilder, capturedType, expectedType, position, isReceiver, isDispatch, nullableExpectedType, sink)
}

fun Candidate.prepareCapturedType(argumentType: ConeKotlinType): ConeKotlinType {
    return captureTypeFromExpressionOrNull(argumentType) ?: argumentType
}

private fun Candidate.captureTypeFromExpressionOrNull(argumentType: ConeKotlinType): ConeKotlinType? {
    if (argumentType is ConeFlexibleType) {
        return captureTypeFromExpressionOrNull(argumentType.lowerBound)
    }

    if (argumentType.typeArguments.isEmpty() || argumentType !is ConeClassLikeType) return null

    return bodyResolveComponents.inferenceComponents.ctx.captureFromArguments(
        argumentType, CaptureStatus.FROM_EXPRESSION
    ) as? ConeKotlinType
}

private fun checkApplicabilityForArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    position: SimpleConstraintSystemConstraintPosition,
    isReceiver: Boolean,
    isDispatch: Boolean,
    nullableExpectedType: ConeKotlinType,
    sink: CheckerSink
) {
    if (isReceiver && isDispatch) {
        if (!expectedType.isNullable && argumentType.isMarkedNullable) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
        }
        return
    }
    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            return
        }
        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER) // TODO
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
        }
    }
}

internal fun Candidate.resolveArgument(
    argument: FirExpression,
    parameter: FirValueParameter,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    sink: CheckerSink
) {

    val expectedType = prepareExpectedType(sink.components.session, argument, parameter)
    resolveArgumentExpression(
        this.system.getBuilder(),
        argument,
        expectedType,
        parameter.returnTypeRef,
        sink,
        isReceiver,
        false,
        isSafeCall
    )
}

private fun Candidate.prepareExpectedType(session: FirSession, argument: FirExpression, parameter: FirValueParameter): ConeKotlinType {
    if (parameter.returnTypeRef is FirILTTypeRefPlaceHolder) return argument.resultType.coneTypeUnsafe()
    val basicExpectedType = argument.getExpectedType(session, parameter/*, LanguageVersionSettings*/)
    val expectedType = getExpectedTypeWithSAMConversion(session, argument, basicExpectedType) ?: basicExpectedType
    return this.substitutor.substituteOrSelf(expectedType)
}

private fun Candidate.getExpectedTypeWithSAMConversion(
    session: FirSession,
    argument: FirExpression,
    candidateExpectedType: ConeKotlinType
): ConeKotlinType? {
    if (candidateExpectedType.isBuiltinFunctionalType(session)) return null
    // TODO: if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument)) return null
    val firFunction = symbol.fir as? FirFunction<*> ?: return null
    if (!samResolver.shouldRunSamConversionForFunction(firFunction)) return null

    val argumentIsFunctional = when ((argument as? FirWrappedArgumentExpression)?.expression ?: argument) {
        is FirAnonymousFunction, is FirCallableReferenceAccess -> true
        else -> argument.typeRef.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true
    }
    if (!argumentIsFunctional) return null

    // TODO: resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    return samResolver.getFunctionTypeForPossibleSamType(candidateExpectedType).apply {
        usesSAM = true
    } ?: return null
}

internal fun FirExpression.getExpectedType(
    session: FirSession,
    parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/
): ConeKotlinType {
    val shouldUnwrapVarargType = when (this) {
        is FirSpreadArgumentExpression -> !isSpread
        is FirNamedArgumentExpression -> false
        else -> true
    }

    return if (parameter.isVararg && shouldUnwrapVarargType) {
        parameter.returnTypeRef.coneTypeUnsafe<ConeKotlinType>().varargElementType(session)
    } else {
        parameter.returnTypeRef.coneTypeUnsafe()
    }
}


fun ConeKotlinType.varargElementType(session: FirSession): ConeKotlinType {
    return this.arrayElementType(session) ?: error("Failed to extract! ${this.render()}!")
}

fun FirTypeRef.isExtensionFunctionType(session: FirSession): Boolean {
    if (annotations.any(FirAnnotationCall::isExtensionFunctionAnnotationCall)) return true

    if (this !is FirResolvedTypeRef) return false
    val type = type as? ConeClassLikeType ?: return false
    if (type.fullyExpandedType(session) === type) return false

    val typeAlias = type.lookupTag
        .toSymbol(session)
        ?.safeAs<FirTypeAliasSymbol>()?.fir ?: return false

    if (typeAlias.expandedTypeRef.annotations.any(FirAnnotationCall::isExtensionFunctionAnnotationCall)) return true

    return false
}