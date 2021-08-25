/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.lookupTracker
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.createFunctionalType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.TypeSystemCommonSuperTypesContext
import org.jetbrains.kotlin.utils.addToStdlib.runIf

val SAM_LOOKUP_NAME = Name.special("<SAM-CONSTRUCTOR>")

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
                    argument,
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
        is FirAnonymousFunctionExpression -> preprocessLambdaArgument(csBuilder, argument, expectedType, expectedTypeRef, context, sink)
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
            block,
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
    require(argument is FirExpression)
    val candidate = argument.candidate() ?: return resolvePlainExpressionArgument(
        csBuilder,
        argument,
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
    val type: ConeKotlinType = context.returnTypeCalculator.tryCalculateReturnType(candidate.symbol.fir as FirTypedDeclaration).type
    val argumentType = candidate.substitutor.substituteOrSelf(type)
    resolvePlainArgumentType(
        csBuilder,
        argument,
        argumentType,
        expectedType,
        sink,
        context,
        isReceiver,
        isDispatch,
        useNullableArgumentType
    )
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
    resolvePlainArgumentType(
        csBuilder,
        argument,
        argumentType,
        expectedType,
        sink,
        context,
        isReceiver,
        isDispatch,
        useNullableArgumentType
    )
}

fun Candidate.resolvePlainArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
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
        csBuilder, argument, argumentTypeForApplicabilityCheck, expectedType, position, isReceiver, isDispatch, sink, context
    )
}

private fun argumentTypeWithSuspendConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedType: ConeKotlinType,
    argumentType: ConeKotlinType
): ConeKotlinType? {
    // TODO: should refer to LanguageVersionSettings.SuspendConversion

    // Expect the expected type to be a suspend functional type.
    if (!expectedType.isSuspendFunctionType(session)) {
        return null
    }

    // We want to check the argument type against non-suspend functional type.
    val expectedFunctionalType = expectedType.suspendFunctionTypeToFunctionType(session)

    val argumentTypeWithInvoke = argumentType.findSubtypeOfNonSuspendFunctionalType(session, expectedFunctionalType)

    return argumentTypeWithInvoke?.findContributedInvokeSymbol(
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

fun Candidate.prepareCapturedType(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType {
    return captureTypeFromExpressionOrNull(argumentType, context) ?: argumentType
}

private fun Candidate.captureTypeFromExpressionOrNull(argumentType: ConeKotlinType, context: ResolutionContext): ConeKotlinType? {
    if (argumentType is ConeFlexibleType) {
        return captureTypeFromExpressionOrNull(argumentType.lowerBound, context)
    }

    if (argumentType is ConeIntersectionType) {
        val intersectedTypes = argumentType.intersectedTypes.map { captureTypeFromExpressionOrNull(it, context) ?: it }
        if (intersectedTypes == argumentType.intersectedTypes) return null
        return ConeIntersectionType(
            intersectedTypes,
            argumentType.alternativeType?.let { captureTypeFromExpressionOrNull(it, context) ?: it }
        )
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

fun isArgumentTypeMismatchDueToNullability(
    argumentType: ConeKotlinType,
    actualExpectedType: ConeKotlinType,
    typeContext: ConeTypeContext
): Boolean {
    return AbstractTypeChecker.isSubtypeOf(
        typeContext,
        argumentType,
        actualExpectedType.withNullability(ConeNullability.NULLABLE, typeContext)
    )
}

private fun checkApplicabilityForArgumentType(
    csBuilder: ConstraintSystemBuilder,
    argument: FirExpression,
    argumentTypeBeforeCapturing: ConeKotlinType,
    expectedType: ConeKotlinType?,
    position: SimpleConstraintSystemConstraintPosition,
    isReceiver: Boolean,
    isDispatch: Boolean,
    sink: CheckerSink,
    context: ResolutionContext
) {
    if (expectedType == null) return

    // todo run this approximation only once for call
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(argumentTypeBeforeCapturing, expectedType, context.session)

    fun subtypeError(actualExpectedType: ConeKotlinType): ResolutionDiagnostic {
        if (argument.isNullLiteral && actualExpectedType.nullability == ConeNullability.NOT_NULL) {
            return NullForNotNullType(argument)
        }

        fun tryGetConeTypeThatCompatibleWithKtType(type: ConeKotlinType): ConeKotlinType {
            if (type is ConeTypeVariableType) {
                val originalTypeParameter =
                    (type.lookupTag as? ConeTypeVariableTypeConstructor)?.originalTypeParameter as? ConeTypeParameterLookupTag

                if (originalTypeParameter != null)
                    return ConeTypeParameterTypeImpl(originalTypeParameter, type.isNullable, type.attributes)
            } else if (type is ConeIntegerLiteralType) {
                return type.possibleTypes.firstOrNull() ?: type
            }

            return type
        }


        return ArgumentTypeMismatch(
            tryGetConeTypeThatCompatibleWithKtType(actualExpectedType),
            tryGetConeTypeThatCompatibleWithKtType(argumentType),
            argument,
            // Reaching here means argument types mismatch, and we want to record whether it's due to the nullability by checking a subtype
            // relation with nullable expected type.
            isArgumentTypeMismatchDueToNullability(argumentType, actualExpectedType, context.session.typeContext)
        )
    }

    if (isReceiver && isDispatch) {
        if (!expectedType.isNullable && argumentType.isMarkedNullable) {
            sink.reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
        }
        return
    }

    if (!csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) {
        val smartcastExpression = argument as? FirExpressionWithSmartcast
        if (smartcastExpression != null && !smartcastExpression.isStable) {
            val unstableType = smartcastExpression.smartcastType.coneType
            if (csBuilder.addSubtypeConstraintIfCompatible(unstableType, expectedType, position)) {
                sink.reportDiagnostic(UnstableSmartCast(smartcastExpression, expectedType))
                return
            }
        }

        if (!isReceiver) {
            sink.reportDiagnosticIfNotNull(subtypeError(expectedType))
            return
        }

        val nullableExpectedType = expectedType.withNullability(ConeNullability.NULLABLE, context.session.typeContext)

        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, nullableExpectedType, position)) {
            sink.reportDiagnostic(UnsafeCall(argumentType)) // TODO
        } else {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            sink.reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
        }
    }
}

internal fun Candidate.resolveArgument(
    callInfo: CallInfo,
    argument: FirExpression,
    parameter: FirValueParameter?,
    isReceiver: Boolean,
    sink: CheckerSink,
    context: ResolutionContext
) {
    argument.resultType.ensureResolvedTypeDeclaration(context.session)
    val expectedType =
        prepareExpectedType(context.session, context.bodyResolveComponents.scopeSession, callInfo, argument, parameter, context)
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
    scopeSession: ScopeSession,
    callInfo: CallInfo,
    argument: FirExpression,
    parameter: FirValueParameter?,
    context: ResolutionContext
): ConeKotlinType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedTypeForSAMConversion(parameter/*, LanguageVersionSettings*/)

    val expectedType =
        getExpectedTypeWithSAMConversion(session, scopeSession, argument, basicExpectedType, context)?.also {
            session.lookupTracker?.let { lookupTracker ->
                parameter.returnTypeRef.coneType.lowerBoundIfFlexible().classId?.takeIf { !it.isLocal }?.let { classId ->
                    lookupTracker.recordLookup(
                        SAM_LOOKUP_NAME,
                        classId.asString(),
                        callInfo.callSite.source,
                        callInfo.containingFile.source
                    )
                    lookupTracker.recordLookup(
                        classId.shortClassName,
                        classId.packageFqName.asString(),
                        callInfo.callSite.source,
                        callInfo.containingFile.source
                    )
                }
            }
        } ?: basicExpectedType
    return this.substitutor.substituteOrSelf(expectedType)
}

private fun Candidate.getExpectedTypeWithSAMConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    argument: FirExpression,
    candidateExpectedType: ConeKotlinType,
    context: ResolutionContext
): ConeKotlinType? {
    if (candidateExpectedType.isBuiltinFunctionalType(session)) return null
    // TODO: if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument)) return null
    val firFunction = symbol.fir as? FirFunction ?: return null
    if (!context.bodyResolveComponents.samResolver.shouldRunSamConversionForFunction(firFunction)) return null

    // TODO: resolvedCall.registerArgumentWithSamConversion(argument, SamConversionDescription(convertedTypeByOriginal, convertedTypeByCandidate!!))

    val expectedFunctionType = context.bodyResolveComponents.samResolver.getFunctionTypeForPossibleSamType(candidateExpectedType)
    return runIf(argument.isFunctional(session, scopeSession, expectedFunctionType)) {
        expectedFunctionType.apply {
            // Even though the `expectedFunctionalType` could be `null`, we should mark the flag to indicate that the argument is a
            // functional type. That will help avoid ambiguous `invoke` resolutions. See KT-39824
            usesSAM = true
        }
    }
}

fun FirExpression.isFunctional(
    session: FirSession,
    scopeSession: ScopeSession,
    expectedFunctionType: ConeKotlinType?,
): Boolean {
    when (unwrapArgument()) {
        is FirAnonymousFunctionExpression, is FirCallableReferenceAccess -> return true
        else -> {
            // Either a functional type or a subtype of a class that has a contributed `invoke`.
            val coneType = typeRef.coneTypeSafe<ConeKotlinType>() ?: return false
            if (coneType.isBuiltinFunctionalType(session)) {
                return true
            }
            val classLikeExpectedFunctionType = expectedFunctionType?.lowerBoundIfFlexible() as? ConeClassLikeType
            if (classLikeExpectedFunctionType == null || coneType is ConeIntegerLiteralType) {
                return false
            }
            val invokeSymbol =
                coneType.findContributedInvokeSymbol(
                    session, scopeSession, classLikeExpectedFunctionType, shouldCalculateReturnTypesOfFakeOverrides = false
                ) ?: return false
            // Make sure the contributed `invoke` is indeed a wanted functional type by checking if types are compatible.
            val expectedReturnType = classLikeExpectedFunctionType.returnType(session).lowerBoundIfFlexible()
            val returnTypeCompatible =
                expectedReturnType is ConeTypeParameterType ||
                        AbstractTypeChecker.isSubtypeOf(
                            session.inferenceComponents.ctx.newTypeCheckerState(
                                errorTypesEqualToAnything = false,
                                stubTypesEqualToAnything = true
                            ),
                            invokeSymbol.fir.returnTypeRef.coneType,
                            expectedReturnType,
                            isFromNullabilityConstraint = false
                        )
            if (!returnTypeCompatible) {
                return false
            }
            if (invokeSymbol.fir.valueParameters.size != classLikeExpectedFunctionType.typeArguments.size - 1) {
                return false
            }
            val parameterPairs =
                invokeSymbol.fir.valueParameters.zip(classLikeExpectedFunctionType.valueParameterTypesIncludingReceiver(session))
            return parameterPairs.all { (invokeParameter, expectedParameter) ->
                val expectedParameterType = expectedParameter.lowerBoundIfFlexible()
                expectedParameterType is ConeTypeParameterType ||
                        AbstractTypeChecker.isSubtypeOf(
                            session.inferenceComponents.ctx.newTypeCheckerState(
                                errorTypesEqualToAnything = false,
                                stubTypesEqualToAnything = true
                            ),
                            invokeParameter.returnTypeRef.coneType,
                            expectedParameterType,
                            isFromNullabilityConstraint = false
                        )
            }
        }
    }
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

    val context = session.typeContext

    val chosenSupertype = typeParameter.bounds.map { it.coneType }
        .singleOrNull { it.hasSupertypeWithGivenClassId(expectedTypeClassId, context) } ?: return argumentType

    val capturedType = context.captureFromExpression(chosenSupertype) as ConeKotlinType? ?: return argumentType
    return if (argumentType is ConeDefinitelyNotNullType) {
        ConeDefinitelyNotNullType.create(capturedType, session.typeContext) ?: capturedType
    } else {
        capturedType
    }
}

private fun ConeKotlinType.hasSupertypeWithGivenClassId(classId: ClassId, context: TypeSystemCommonSuperTypesContext): Boolean {
    return with(context) {
        anySuperTypeConstructor {
            it is ConeClassLikeLookupTag && it.classId == classId
        }
    }
}
