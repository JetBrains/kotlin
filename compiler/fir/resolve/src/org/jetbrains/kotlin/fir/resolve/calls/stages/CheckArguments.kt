/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
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
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.isSubtypeConstraintCompatible
import org.jetbrains.kotlin.types.model.typeConstructor

internal object CheckArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        val argumentMapping = candidate.argumentMapping
        val isInvokeFromExtensionFunctionType = candidate.isInvokeFromExtensionFunctionType

        val contextArgumentsOfInvoke = candidate.expectedContextParameterTypesForInvoke?.size ?: 0
        for ((index, argument) in candidate.arguments.withIndex()) {
            if (index < contextArgumentsOfInvoke) continue
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
            prepareExpectedType(context.session, callInfo, argument, parameter, context)
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
    callInfo: CallInfo,
    argument: FirExpression,
    parameter: FirValueParameter?,
    context: ResolutionContext
): ConeKotlinType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedType(session, parameter/*, LanguageVersionSettings*/)

    val expectedType =
        getExpectedTypeWithSAMConversion(session, argument, basicExpectedType, context)?.also {
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
            candidateExpectedType = candidateExpectedType,
            expectedFunctionType = expectedFunctionType,
            this,
        )
    ) {
        return null
    }

    setSamConversionOfArgument(argument.unwrapArgument(), samConversionInfo)
    return expectedFunctionType
}

private fun FirExpression.shouldUseSamConversion(
    session: FirSession,
    candidateExpectedType: ConeKotlinType,
    expectedFunctionType: ConeKotlinType,
    candidate: Candidate,
): Boolean {
    val unwrapped = unwrapArgument()

    // Always apply SAM conversion on lambdas and callable references
    if (unwrapped is FirAnonymousFunctionExpression || unwrapped is FirCallableReferenceAccess) {
        return true
    }

    // Always apply SAM conversion on generic call with nested lambda like `run { {} }`
    if (unwrapped.isCallWithGenericReturnTypeAndMatchingLambda(session)) {
        return true
    }

    val expressionType = resolvedType
    if (expressionType is ConeIntegerLiteralType) {
        return false
    }

    // Expression type is a subtype of expected type, no need for SAM conversion.
    val substitutedExpectedType = candidate.substitutor.substituteOrSelf(candidateExpectedType)
    if (candidate.csBuilder.isSubtypeConstraintCompatible(expressionType, substitutedExpectedType)) {
        return false
    }

    if (expressionType.isSomeFunctionType(session)) {
        return true
    }

    // If the expression type is compatible with the expected function type, apply SAM conversion
    val substitutedExpectedFunctionType = candidate.substitutor.substituteOrSelf(expectedFunctionType)
    if (candidate.csBuilder.isSubtypeConstraintCompatible(expressionType, substitutedExpectedFunctionType)) {
        return true
    }

    // If the expected function type is a non-basic function type, check if the expression type is compatible with its basic version
    // If yes, apply SAM conversion (with function kind conversion).
    if (!substitutedExpectedFunctionType.isBasicFunctionOrKFunctionType(session)) {
        val expectedFunctionTypeAsSimpleFunctionType = substitutedExpectedFunctionType
            .lowerBoundIfFlexible()
            // converts SuspendFunction/[Custom]Function -> Function
            .customFunctionTypeToSimpleFunctionType(session)

        if (candidate.csBuilder.isSubtypeConstraintCompatible(expressionType, expectedFunctionTypeAsSimpleFunctionType)) {
            return true
        }
    }

    return false
}

/**
 * Returns true if this expression is a call having a type variable as return type,
 * and at the same time that type variable is the expected type of a not-yet analyzed lambda.
 *
 * In other words, the argument is some generic call that encapsulates a nested lambda, e.g. `outerCall(argument = run { { ... } })`
 * and the expression type is likely determined by the nested lambda, therefore, it will be a function type.
 *
 * In this case, we should treat it analogously to a simple lambda argument (`outerCall {}`) and apply SAM conversion.
 */
private fun FirExpression.isCallWithGenericReturnTypeAndMatchingLambda(session: FirSession): Boolean {
    val expressionType = resolvedType
    val postponedAtoms = namedReferenceWithCandidate()?.candidate?.postponedAtoms ?: return false
    return postponedAtoms.any {
        it is ConeLambdaWithTypeVariableAsExpectedTypeAtom &&
                it.expectedType.typeConstructor(session.typeContext) == expressionType.typeConstructor(session.typeContext)
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

    return argumentType?.withNullabilityOf(candidateExpectedType, session.typeContext)
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
