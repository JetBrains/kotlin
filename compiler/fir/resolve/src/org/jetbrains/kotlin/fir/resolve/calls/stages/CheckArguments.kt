/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.typeConstructor

internal object CheckArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        val argumentMapping = candidate.argumentMapping
        val isInvokeFromExtensionFunctionType = candidate.isInvokeFromExtensionFunctionType

        for ((index, argument) in candidate.arguments.withIndex()) {
            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                callInfo,
                argument,
                parameter,
                isReceiver = index == 0 && isInvokeFromExtensionFunctionType,
                sink = sink,
                context = context
            )
        }

        when {
            candidate.system.hasContradiction && callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }

            // Logic description: only candidates from Kotlin, but using Java SAM types, are discriminated
            candidate.shouldHaveLowPriorityDueToSAM(context.bodyResolveComponents) -> {
                if (argumentMapping.values.any {
                        val coneType = it.returnTypeRef.coneType
                        context.bodyResolveComponents.samResolver.isSamType(coneType) &&
                                // Candidate is not from Java, so no flexible types are possible here
                                coneType.toRegularClassSymbol(context.session)?.isJavaOrEnhancement == true
                    }
                ) {
                    sink.markCandidateForCompatibilityResolve(context)
                }
            }
        }
    }

    private fun Candidate.resolveArgument(
        callInfo: CallInfo,
        atom: ConeResolutionAtom,
        parameter: FirValueParameter?,
        isReceiver: Boolean,
        sink: CheckerSink,
        context: ResolutionContext
    ) {
        // Lambdas and callable references can be unresolved at this point
        val argument = atom.expression
        @OptIn(UnresolvedExpressionTypeAccess::class)
        argument.coneTypeOrNull.ensureResolvedTypeDeclaration(context.session)
        val expectedType =
            prepareExpectedType(context.session, context.bodyResolveComponents.scopeSession, callInfo, argument, parameter, context)
        ArgumentCheckingProcessor.resolveArgumentExpression(
            this,
            atom,
            expectedType,
            sink,
            context,
            isReceiver,
            false
        )
    }
}

private val SAM_LOOKUP_NAME: Name = Name.special("<SAM-CONSTRUCTOR>")

private fun Candidate.prepareExpectedType(
    session: FirSession,
    scopeSession: ScopeSession,
    callInfo: CallInfo,
    argument: FirExpression,
    parameter: FirValueParameter?,
    context: ResolutionContext
): ConeKotlinType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedType(session, parameter/*, LanguageVersionSettings*/)

    val expectedType =
        getExpectedTypeWithSAMConversion(session, scopeSession, argument, basicExpectedType, context)?.also {
            session.lookupTracker?.let { lookupTracker ->
                parameter.returnTypeRef.coneType.lowerBoundIfFlexible().classId?.takeIf { !it.isLocal }?.let { classId ->
                    lookupTracker.recordClassMemberLookup(
                        SAM_LOOKUP_NAME.asString(),
                        classId,
                        callInfo.callSite.source,
                        callInfo.containingFile.source
                    )
                    lookupTracker.recordClassLikeLookup(classId, callInfo.callSite.source, callInfo.containingFile.source)
                }
            }
        }
            ?: getExpectedTypeWithImplicitIntegerCoercion(session, argument, parameter, basicExpectedType)
            ?: basicExpectedType
    return this.substitutor.substituteOrSelf(expectedType)
}

private fun Candidate.getExpectedTypeWithSAMConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    argument: FirExpression,
    candidateExpectedType: ConeKotlinType,
    context: ResolutionContext,
): ConeKotlinType? {
    if (candidateExpectedType.isSomeFunctionType(session)) return null

    val samConversionInfo = context.bodyResolveComponents.samResolver.getSamInfoForPossibleSamType(candidateExpectedType)
        ?: return null
    val expectedFunctionType = samConversionInfo.functionalType

    if (!argument.shouldUseSamConversion(
            session,
            scopeSession,
            candidateExpectedType = candidateExpectedType,
            expectedFunctionType = expectedFunctionType,
            context.returnTypeCalculator
        )
    ) {
        return null
    }

    val samConversions = functionTypesOfSamConversions
        ?: hashMapOf<FirExpression, FirSamResolver.SamConversionInfo>().also { initializeFunctionTypesOfSamConversions(it) }

    samConversions[argument.unwrapArgument()] = samConversionInfo
    return expectedFunctionType
}

private fun FirExpression.shouldUseSamConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    candidateExpectedType: ConeKotlinType,
    expectedFunctionType: ConeKotlinType,
    returnTypeCalculator: ReturnTypeCalculator,
): Boolean {
    when (val unwrapped = unwrapArgument()) {
        is FirAnonymousFunctionExpression, is FirCallableReferenceAccess -> return true
        else -> {
            // Either a functional type or a subtype of a class that has a contributed `invoke`.
            val coneType = resolvedType
            // Argument might have an intersection type between FunctionN and the SAM type from a smart cast, in which case we don't want to use
            // SAM conversion.
            if (coneType.isSubtypeOf(candidateExpectedType, session)) {
                return false
            }
            if (coneType.isSomeFunctionType(session)) {
                return true
            }
            val classLikeExpectedFunctionType = expectedFunctionType.lowerBoundIfFlexible() as? ConeClassLikeType
            if (classLikeExpectedFunctionType == null || coneType is ConeIntegerLiteralType) {
                return false
            }

            val namedReferenceWithCandidate = unwrapped.namedReferenceWithCandidate()
            if (namedReferenceWithCandidate?.candidate?.postponedAtoms?.any {
                    it is ConeLambdaWithTypeVariableAsExpectedTypeAtom &&
                            it.expectedType.typeConstructor(session.typeContext) == coneType.typeConstructor(session.typeContext)
                } == true
            ) {
                return true
            }
            return isSubtypeForSamConversion(session, scopeSession, coneType, classLikeExpectedFunctionType, returnTypeCalculator)
        }
    }
}

/**
 * This function checks whether an actual expression type is a subtype of an expected functional type in context of SAM conversion
 *
 * In more details, this function searches for an invoke symbol inside the actual expression type,
 * and then checks compatibility of invoke symbol parameter types and return type
 * with the corresponding functional type parameters and return type. During this check,
 * type parameters inside the expected functional type are considered as compatible with everything;
 * also, stub types in any positions are considered equal to anything.
 * In other aspects, a normal subtype check is used.
 */
private fun isSubtypeForSamConversion(
    session: FirSession,
    scopeSession: ScopeSession,
    actualExpressionType: ConeKotlinType,
    classLikeExpectedFunctionType: ConeClassLikeType,
    returnTypeCalculator: ReturnTypeCalculator
): Boolean {
    // TODO: can we replace the function with a call of ConeKotlinType.isSubtypeOfFunctionType from FunctionalTypeUtils.kt,
    // or with a call of AbstractTypeChecker.isSubtypeOf ?
    // Relevant tests that can become broken:
    // - codegen/box/sam/passSubtypeOfFunctionSamConversion.kt
    // - diagnostics/tests/j+k/sam/recursiveSamsAndInvoke.kt
    val invokeSymbol =
        actualExpressionType.findContributedInvokeSymbol(
            session, scopeSession, classLikeExpectedFunctionType, shouldCalculateReturnTypesOfFakeOverrides = false
        ) ?: return false
    // Make sure the contributed `invoke` is indeed a wanted functional type by checking if types are compatible.
    val expectedReturnType = classLikeExpectedFunctionType.returnType(session).lowerBoundIfFlexible()
    val returnTypeCompatible =
        // TODO: can we remove is ConeTypeParameterType check here?
        expectedReturnType.originalIfDefinitelyNotNullable() is ConeTypeParameterType ||
                AbstractTypeChecker.isSubtypeOf(
                    session.typeContext.newTypeCheckerState(
                        errorTypesEqualToAnything = false,
                        stubTypesEqualToAnything = true
                    ),
                    // TODO: can we remove returnTypeCalculatorFrom here
                    returnTypeCalculator.tryCalculateReturnType(invokeSymbol.fir).type,
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
        // TODO: can we remove is ConeTypeParameterType check here?
        expectedParameterType.originalIfDefinitelyNotNullable() is ConeTypeParameterType ||
                AbstractTypeChecker.isSubtypeOf(
                    session.typeContext.newTypeCheckerState(
                        errorTypesEqualToAnything = false,
                        stubTypesEqualToAnything = true
                    ),
                    invokeParameter.returnTypeRef.coneType,
                    expectedParameterType,
                    isFromNullabilityConstraint = false
                )
    }
}

private fun getExpectedTypeWithImplicitIntegerCoercion(
    session: FirSession,
    argument: FirExpression,
    parameter: FirValueParameter,
    candidateExpectedType: ConeKotlinType
): ConeKotlinType? {
    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return null

    if (!parameter.isMarkedWithImplicitIntegerCoercion) return null
    if (!candidateExpectedType.fullyExpandedType(session).isUnsignedTypeOrNullableUnsignedType) return null

    val argumentType =
        if (argument.isIntegerLiteralOrOperatorCall()) {
            argument.resolvedType
        } else {
            argument.toReference(session)?.toResolvedCallableSymbol()?.takeIf {
                it.rawStatus.isConst && it.isMarkedWithImplicitIntegerCoercion
            }?.resolvedReturnType
        }

    return argumentType?.withNullability(candidateExpectedType.nullability, session.typeContext)
}

private fun FirExpression.namedReferenceWithCandidate(): FirNamedReferenceWithCandidate? =
    when (this) {
        is FirResolvable -> calleeReference as? FirNamedReferenceWithCandidate
        is FirSafeCallExpression -> (selector as? FirExpression)?.namedReferenceWithCandidate()
        else -> null
    }

private fun CheckerSink.markCandidateForCompatibilityResolve(context: ResolutionContext) {
    if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
    reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
}
