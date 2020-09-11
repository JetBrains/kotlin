/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.isLocalClassOrAnonymousObject
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.scopes.impl.FirILTTypeRefPlaceHolder
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
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
    val type: ConeKotlinType = if (candidate.symbol.fir is FirIntegerOperator) {
        (argument as FirFunctionCall).resultType.coneType
    } else {
        context.returnTypeCalculator.tryCalculateReturnType(candidate.symbol.firUnsafe()).type
    }
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
    checkApplicabilityForIntegerOperatorCall(sink, argument)
}

private fun Candidate.checkApplicabilityForIntegerOperatorCall(sink: CheckerSink, argument: FirExpression) {
    if (symbol.fir !is FirIntegerOperator) return
    if (argument !is FirConstExpression<*> && argument !is FirIntegerOperatorCall) {
        sink.reportDiagnostic(InapplicableCandidate)
    }
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
        argumentTypeWithSuspendConversion(session, expectedType, argumentTypeForApplicabilityCheck)?.let {
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
    session: FirSession, expectedType: ConeKotlinType, argumentType: ConeKotlinType
): ConeKotlinType? {
    // TODO: should refer to LanguageVersionSettings.SuspendConversion
    // Expect the expected type to be a suspend functional type.
    if (!expectedType.isSuspendFunctionType(session)) {
        return null
    }

    // Either the argument type is a non-suspend functional type
    if (argumentType.isBuiltinFunctionalType(session) && !argumentType.isSuspendFunctionType(session)) {
        val typeParameters = argumentType.typeArguments.map { it as ConeKotlinType }
        return createFunctionalType(
            typeParameters.dropLast(1), null, typeParameters.last(),
            isSuspend = true,
            isKFunctionType = argumentType.isKFunctionType(session)
        )
    }

    // Or is a class-like type whose corresponding class is a user-defined and has an overridden `invoke`.
    if (argumentType is ConeClassLikeType) {
        val klass =
            (session.firSymbolProvider.getClassLikeSymbolByFqName(argumentType.lookupTag.classId) as? FirRegularClassSymbol)?.fir
                ?: return null
        if (klass.origin != FirDeclarationOrigin.Source) {
            return null
        }
        return klass.declarations
            .filterIsInstance<FirSimpleFunction>()
            .singleOrNull { it.name == Name.identifier("invoke") && it.valueParameters.size == expectedType.typeArguments.size - 1 }
            ?.let { invokeFunction ->
                createFunctionalType(
                    invokeFunction.valueParameters.map { it.returnTypeRef.coneType },
                    null,
                    invokeFunction.returnTypeRef.coneType,
                    isSuspend = true,
                    isKFunctionType = false
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
    val parameterReturnTypeRef = parameter.returnTypeRef
    if (parameterReturnTypeRef is FirILTTypeRefPlaceHolder && argument.resultType is FirResolvedTypeRef)
        return argument.resultType.coneType.takeIf { it in parameterReturnTypeRef.type.possibleTypes } ?: session.builtinTypes.intType.type
    val basicExpectedType = argument.getExpectedType(session, parameter/*, LanguageVersionSettings*/)
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

internal fun FirExpression.getExpectedType(
    session: FirSession,
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
