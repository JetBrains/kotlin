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
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun Candidate.resolveArgumentExpression(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean
) {
    when (argument) {
        is FirFunctionCall, is FirWhenExpression, is FirTryExpression, is FirCheckNotNullCall -> resolveSubCallArgument(
            csBuilder,
            argument as FirResolvable,
            expectedType,
            sink,
            isReceiver,
            isDispatch
        )
        // x?.bar() is desugared to `x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() }`
        //
        // If we have a safe-call as argument like in a call "foo(x SAFE-CALL-OPERATOR { $not-null-receiver$.bar() })"
        // we obtain argument type (and argument's constraint system) from "$not-null-receiver$.bar()" (argument.regularQualifiedAccess)
        // and then add constraint: typeOf(`$not-null-receiver$.bar()`).makeNullable() <: EXPECTED_TYPE
        // NB: argument.regularQualifiedAccess is either a call or a qualified access
        is FirSafeCallExpression -> {
            val nestedQualifier = argument.regularQualifiedAccess
            if (nestedQualifier is FirExpression) {
                resolveSubCallArgument(
                    csBuilder,
                    nestedQualifier,
                    expectedType,
                    sink,
                    isReceiver,
                    isDispatch,
                    useNullableArgumentType = true
                )
            } else {
                // Assignment
                checkApplicabilityForArgumentType(
                    csBuilder,
                    StandardClassIds.Unit.constructClassLikeType(emptyArray(), isNullable = false),
                    expectedType?.type,
                    SimpleConstraintSystemConstraintPosition,
                    isReceiver = false,
                    isDispatch = false,
                    sink = sink
                )
            }
        }
        is FirCallableReferenceAccess ->
            if (argument.calleeReference is FirResolvedNamedReference)
                resolvePlainExpressionArgument(
                    csBuilder,
                    argument,
                    expectedType,
                    sink,
                    isReceiver,
                    isDispatch
                )
            else
                preprocessCallableReference(argument, expectedType)
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
            isDispatch
        )
        is FirBlock -> resolveBlockArgument(
            csBuilder,
            argument,
            expectedType,
            expectedTypeRef,
            sink,
            isReceiver,
            isDispatch
        )
        else -> resolvePlainExpressionArgument(csBuilder, argument, expectedType, sink, isReceiver, isDispatch)
    }
}

private fun Candidate.resolveBlockArgument(
    csBuilder: ConstraintSystemBuilder,
    block: FirBlock,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean
) {
    val returnArguments = block.returnExpressions()
    if (returnArguments.isEmpty()) {
        checkApplicabilityForArgumentType(
            csBuilder,
            block.typeRef.coneTypeUnsafe(),
            expectedType?.type,
            SimpleConstraintSystemConstraintPosition,
            isReceiver = false,
            isDispatch = false,
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
            isDispatch
        )
    }
}

fun Candidate.resolveSubCallArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirResolvable,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    val candidate = argument.candidate() ?: return resolvePlainExpressionArgument(
        csBuilder,
        argument as FirExpression,
        expectedType,
        sink,
        isReceiver,
        isDispatch,
        useNullableArgumentType
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
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isDispatch, useNullableArgumentType)
}

fun Candidate.resolvePlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    if (expectedType == null) return
    val argumentType = argument.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, isReceiver, isDispatch, useNullableArgumentType)
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
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    val position = SimpleConstraintSystemConstraintPosition //TODO

    val session = sink.components.session
    val capturedType = prepareCapturedType(argumentType)

    val argumentTypeForApplicabilityCheck =
        if (useNullableArgumentType)
            capturedType.withNullability(ConeNullability.NULLABLE, session.inferenceContext)
        else
            capturedType

    checkApplicabilityForArgumentType(
        csBuilder, argumentTypeForApplicabilityCheck, expectedType, position, isReceiver, isDispatch, sink
    )
}

fun Candidate.prepareCapturedType(argumentType: ConeKotlinType): ConeKotlinType {
    return captureTypeFromExpressionOrNull(argumentType) ?: argumentType
}

private fun Candidate.captureTypeFromExpressionOrNull(argumentType: ConeKotlinType): ConeKotlinType? {
    if (argumentType is ConeFlexibleType) {
        return captureTypeFromExpressionOrNull(argumentType.lowerBound)
    }

    if (argumentType !is ConeClassLikeType) return null

    argumentType.fullyExpandedType(bodyResolveComponents.session).let {
        if (it !== argumentType) return captureTypeFromExpressionOrNull(it)
    }

    if (argumentType.typeArguments.isEmpty()) return null

    return bodyResolveComponents.inferenceComponents.ctx.captureFromArguments(
        argumentType, CaptureStatus.FROM_EXPRESSION
    ) as? ConeKotlinType
}

private fun checkApplicabilityForArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType?,
    position: SimpleConstraintSystemConstraintPosition,
    isReceiver: Boolean,
    isDispatch: Boolean,
    sink: CheckerSink
) {
    if (expectedType == null) return
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

        val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, sink.components.session.inferenceContext)

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
    parameter: FirValueParameter?,
    isReceiver: Boolean,
    isSafeCall: Boolean,
    sink: CheckerSink
) {

    val expectedType = prepareExpectedType(sink.components.session, argument, parameter)
    resolveArgumentExpression(
        this.system.getBuilder(),
        argument,
        expectedType,
        parameter?.returnTypeRef,
        sink,
        isReceiver,
        false
    )
}

private fun Candidate.prepareExpectedType(session: FirSession, argument: FirExpression, parameter: FirValueParameter?): ConeKotlinType? {
    if (parameter == null) return null
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

    if (!argument.isFunctional(session)) return null

    // TODO: resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    return samResolver.getFunctionTypeForPossibleSamType(candidateExpectedType).apply {
        usesSAM = true
    } ?: return null
}

fun FirExpression.isFunctional(session: FirSession): Boolean =
    when ((this as? FirWrappedArgumentExpression)?.expression ?: this) {
        is FirAnonymousFunction, is FirCallableReferenceAccess -> true
        else -> typeRef.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true
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
