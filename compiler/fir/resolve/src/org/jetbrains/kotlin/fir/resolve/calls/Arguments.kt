/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.typeCheckerContext
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.CaptureStatus

fun Candidate.resolveArgumentExpression(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    sink: CheckerSink,
    context: ResolutionContext,
    isReceiver: Boolean,
    isDispatch: Boolean
) {
    when (argument) {
        is FirFunctionCall, is FirWhenExpression, is FirTryExpression, is FirCheckNotNullCall, is FirElvisExpression -> resolveSubCallArgument(
            csBuilder,
            argument as FirResolvable,
            expectedType,
            sink,
            context,
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
                    context,
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
                    sink = sink,
                    context = context
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
                    context,
                    isReceiver,
                    isDispatch
                )
            else
                preprocessCallableReference(argument, expectedType, context)
        // TODO:!
        is FirAnonymousFunction -> preprocessLambdaArgument(csBuilder, argument, expectedType, expectedTypeRef, context)
        // TODO:!
        //TODO: Collection literal
        is FirWrappedArgumentExpression -> resolveArgumentExpression(
            csBuilder,
            argument.expression,
            expectedType,
            expectedTypeRef,
            sink,
            context,
            isReceiver,
            isDispatch
        )
        is FirBlock -> resolveBlockArgument(
            csBuilder,
            argument,
            expectedType,
            expectedTypeRef,
            sink,
            context,
            isReceiver,
            isDispatch
        )
        else -> resolvePlainExpressionArgument(csBuilder, argument, expectedType, sink, context, isReceiver, isDispatch)
    }
}

private fun Candidate.resolveBlockArgument(
    csBuilder: ConstraintSystemBuilder,
    block: FirBlock,
    expectedType: ConeKotlinType?,
    expectedTypeRef: FirTypeRef?,
    sink: CheckerSink,
    context: ResolutionContext,
    isReceiver: Boolean,
    isDispatch: Boolean
) {
    val returnArguments = block.returnExpressions()
    if (returnArguments.isEmpty()) {
        checkApplicabilityForArgumentType(
            csBuilder,
            block.typeRef.coneType,
            expectedType?.type,
            SimpleConstraintSystemConstraintPosition,
            isReceiver = false,
            isDispatch = false,
            sink = sink,
            context = context
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
            context,
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
    context: ResolutionContext,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    val candidate = argument.candidate() ?: return resolvePlainExpressionArgument(
        csBuilder,
        argument as FirExpression,
        expectedType,
        sink,
        context,
        isReceiver,
        isDispatch,
        useNullableArgumentType
    )
    /*
     * It's important to extract type from argument neither from symbol, because of symbol contains
     *   placeholder type with value 0, but argument contains type with proper literal value
     */
    val type: ConeKotlinType = context.returnTypeCalculator.tryCalculateReturnType(candidate.symbol.firUnsafe()).type
    val argumentType = candidate.substitutor.substituteOrSelf(type)
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, context, isReceiver, isDispatch, useNullableArgumentType)
}

fun Candidate.resolvePlainExpressionArgument(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    context: ResolutionContext,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    if (expectedType == null) return
    val argumentType = argument.typeRef.coneTypeSafe<ConeKotlinType>() ?: return
    resolvePlainArgumentType(csBuilder, argumentType, expectedType, sink, context, isReceiver, isDispatch, useNullableArgumentType)
}

fun Candidate.resolvePlainArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType?,
    sink: CheckerSink,
    context: ResolutionContext,
    isReceiver: Boolean,
    isDispatch: Boolean,
    useNullableArgumentType: Boolean = false
) {
    val position = SimpleConstraintSystemConstraintPosition //TODO

    val session = context.session
    val capturedType = prepareCapturedType(argumentType, context)

    var argumentTypeForApplicabilityCheck =
        if (useNullableArgumentType)
            capturedType.withNullability(ConeNullability.NULLABLE, session.typeContext)
        else
            capturedType

    // If the argument is of functional type and the expected type is a suspend function type, we need to do "suspend conversion."
    if (expectedType != null) {
        argumentTypeWithSuspendConversion(
            session, context.bodyResolveComponents.scopeSession, expectedType, argumentTypeForApplicabilityCheck
        )?.let {
            argumentTypeForApplicabilityCheck = it
            substitutor.substituteOrSelf(argumentTypeForApplicabilityCheck)
            usesSuspendConversion = true
        }
    }

    checkApplicabilityForArgumentType(
        csBuilder, argumentTypeForApplicabilityCheck, expectedType, position, isReceiver, isDispatch, sink, context
    )
}

private fun argumentTypeWithSuspendConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedType: ConeKotlinType,
    argumentType: ConeKotlinType
): ConeKotlinType? {
    // TODO: should refer to LanguageVersionSettings.SuspendConversion

    // To avoid any remaining exotic types, e.g., intersection type, like it(FunctionN..., SuspendFunctionN...)
    if (argumentType !is ConeClassLikeType) {
        return null
    }

    // Expect the expected type to be a suspend functional type, and the argument type is not a suspend functional type.
    if (!expectedType.isSuspendFunctionType(session) || argumentType.isSuspendFunctionType(session)) {
        return null
    }

    // We want to check the argument type against non-suspend functional type.
    val expectedFunctionalType = expectedType.suspendFunctionTypeToFunctionType(session)
    if (argumentType.isSubtypeOfFunctionalType(session, expectedFunctionalType)) {
        return argumentType.findContributedInvokeSymbol(
            session,
            scopeSession,
            expectedFunctionalType,
            shouldCalculateReturnTypesOfFakeOverrides = false
        )?.let { invokeSymbol ->
            createFunctionalType(
                invokeSymbol.fir.valueParameters.map { it.returnTypeRef.coneType },
                null,
                invokeSymbol.fir.returnTypeRef.coneType,
                isSuspend = true,
                isKFunctionType = argumentType.isKFunctionType(session)
            )
        }
    }

    return null
}

fun Candidate.prepareCapturedType(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType {
    return captureTypeFromExpressionOrNull(argumentType, context) ?: argumentType
}

private fun Candidate.captureTypeFromExpressionOrNull(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType? {
    if (argumentType is ConeFlexibleType) {
        return captureTypeFromExpressionOrNull(argumentType.lowerBound, context)
    }

    if (argumentType !is ConeClassLikeType) return null

    argumentType.fullyExpandedType(context.session).let {
        if (it !== argumentType) return captureTypeFromExpressionOrNull(it, context)
    }

    if (argumentType.typeArguments.isEmpty()) return null

    return context.inferenceComponents.ctx.captureFromArguments(
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
    sink: CheckerSink,
    context: ResolutionContext
) {
    if (expectedType == null) return
    if (isReceiver && isDispatch) {
        if (!expectedType.isNullable && argumentType.isMarkedNullable) {
            sink.reportDiagnostic(InapplicableWrongReceiver)
        }
        return
    }
    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        if (!isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            return
        }

        val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, context.session.typeContext)

        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportDiagnostic(InapplicableWrongReceiver) // TODO
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            sink.reportDiagnostic(InapplicableWrongReceiver)
        }
    }
}

internal fun Candidate.resolveArgument(
    argument: FirExpression,
    parameter: FirValueParameter?,
    isReceiver: Boolean,
    sink: CheckerSink,
    context: ResolutionContext
) {

    argument.resultType.ensureResolvedTypeDeclaration(context.session)

    val expectedType = prepareExpectedType(context.session, argument, parameter, context)
    resolveArgumentExpression(
        this.system.getBuilder(),
        argument,
        expectedType,
        parameter?.returnTypeRef,
        sink,
        context,
        isReceiver,
        false
    )
}

private fun Candidate.prepareExpectedType(
    session: FirSession,
    argument: FirExpression,
    parameter: FirValueParameter?,
    context: ResolutionContext
): ConeKotlinType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedTypeForSAMConversion(parameter/*, LanguageVersionSettings*/)
    val expectedType = getExpectedTypeWithSAMConversion(session, argument, basicExpectedType, context) ?: basicExpectedType
    return this.substitutor.substituteOrSelf(expectedType)
}

private fun Candidate.getExpectedTypeWithSAMConversion(
    session: FirSession,
    argument: FirExpression,
    candidateExpectedType: ConeKotlinType,
    context: ResolutionContext
): ConeKotlinType? {
    if (candidateExpectedType.isBuiltinFunctionalType(session)) return null
    // TODO: if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument)) return null
    val firFunction = symbol.fir as? FirFunction<*> ?: return null
    if (!context.bodyResolveComponents.samResolver.shouldRunSamConversionForFunction(firFunction)) return null

    if (!argument.isFunctional(session)) return null

    // TODO: resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    return context.bodyResolveComponents.samResolver.getFunctionTypeForPossibleSamType(candidateExpectedType).apply {
        usesSAM = true
    }
}

fun FirExpression.isFunctional(session: FirSession): Boolean =
    when ((this as? FirWrappedArgumentExpression)?.expression ?: this) {
        is FirAnonymousFunction, is FirCallableReferenceAccess -> true
        else -> typeRef.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true
    }

fun FirExpression.getExpectedTypeForSAMConversion(
    parameter: FirValueParameter/*, languageVersionSettings: LanguageVersionSettings*/
): ConeKotlinType {
    val shouldUnwrapVarargType = when (this) {
        is FirSpreadArgumentExpression -> !isSpread
        is FirNamedArgumentExpression -> expression is FirConstExpression<*>
        else -> true
    }

    return if (parameter.isVararg && shouldUnwrapVarargType) {
        parameter.returnTypeRef.coneType.varargElementType()
    } else {
        parameter.returnTypeRef.coneType
    }
}

fun ConeKotlinType.varargElementType(): ConeKotlinType {
    return this.arrayElementType() ?: this
}

/**
 * interface Inv<T>
 * fun <Y> bar(l: Inv<Y>): Y = ...
 *
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val xr = bar(x)
 * }
 * Here we try to capture from upper bound from type parameter.
 * We replace type of `x` to `Inv<out Int>`(we chose supertype which contains supertype with expectedTypeConstructor) and capture from this type.
 * It is correct, because it is like this code:
 * fun <X : Inv<out Int>> foo(x: X) {
 *      val inv: Inv<out Int> = x
 *      val xr = bar(inv)
 * }
 *
 */
internal fun captureFromTypeParameterUpperBoundIfNeeded(
    argumentType: ConeKotlinType,
    expectedType: ConeKotlinType,
    session: FirSession
): ConeKotlinType {
    val expectedTypeClassId = expectedType.upperBoundIfFlexible().classId ?: return argumentType
    val simplifiedArgumentType = argumentType.lowerBoundIfFlexible() as? ConeTypeParameterType ?: return argumentType
    val typeParameter = simplifiedArgumentType.lookupTag.typeParameterSymbol.fir

    val context = session.typeCheckerContext

    val chosenSupertype = typeParameter.bounds.map { it.coneType }
        .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) } ?: return argumentType

    val capturedType = context.captureFromExpression(chosenSupertype) as ConeKotlinType? ?: return argumentType
    return if (argumentType is ConeDefinitelyNotNullType) {
        ConeDefinitelyNotNullType.create(capturedType) ?: capturedType
    } else {
        capturedType
    }
}

private fun ConeKotlinType.hasSupertypeWithGivenClassId(classId: ClassId, context: ConeTypeCheckerContext): Boolean {
    return with(context) {
        anySuperTypeConstructor {
            it is ConeClassLikeLookupTag && it.classId == classId
        }
    }
}
