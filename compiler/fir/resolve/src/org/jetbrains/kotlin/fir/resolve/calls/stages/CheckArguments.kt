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
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
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
import org.jetbrains.kotlin.types.model.anySuperTypeConstructor
import org.jetbrains.kotlin.types.model.typeConstructor

internal object CheckArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        val argumentMapping = candidate.argumentMapping
        val isInvokeFromExtensionFunctionType = candidate.isInvokeFromExtensionFunctionType

        val contextArgumentsOfInvoke = candidate.expectedContextParameterCountForInvoke ?: 0
        for ((index, argument) in candidate.arguments.withIndex()) {
            if (index < contextArgumentsOfInvoke) continue

            val expression = argument.expression
            if (expression.isInaccessibleAndInapplicable()) {
                sink.reportDiagnostic(expression.toInaccessibleReceiverDiagnostic())
            }

            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                candidate.callInfo,
                argument,
                parameter,
                isReceiver = index == 0 && isInvokeFromExtensionFunctionType
            )
        }

        when {
            candidate.system.hasContradiction && candidate.callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }

            // Logic description: only candidates from Kotlin, but using Java SAM types, are discriminated
            candidate.shouldHaveLowPriorityDueToSAM(context.bodyResolveComponents) -> {
                if (argumentMapping.values.any {
                        val coneType = it.returnTypeRef.coneType
                        context.bodyResolveComponents.samResolver.isSamType(coneType) &&
                                // Candidate is not from Java, so no flexible types are possible here
                                coneType.toRegularClassSymbol()?.isJavaOrEnhancement == true
                    }
                ) {
                    sink.markCandidateForCompatibilityResolve()
                }
            }
        }
    }

    context(sink: CheckerSink, context: ResolutionContext)
    private fun Candidate.resolveArgument(
        callInfo: CallInfo,
        atom: ConeResolutionAtom,
        parameter: FirValueParameter?,
        isReceiver: Boolean,
    ) {
        // Lambdas and callable references can be unresolved at this point
        val argument = atom.expression
        @OptIn(UnresolvedExpressionTypeAccess::class)
        argument.coneTypeOrNull.ensureResolvedTypeDeclaration(context.session)
        val expectedType = prepareExpectedType(callInfo, argument, parameter)
        ArgumentCheckingProcessor.resolveArgumentExpression(
            this,
            atom,
            expectedType,
            sink,
            context,
            isReceiver,
            isDispatch = false
        )
    }
}

private val SAM_LOOKUP_NAME: Name = Name.special("<SAM-CONSTRUCTOR>")

context(context: ResolutionContext)
private fun Candidate.prepareExpectedType(
    callInfo: CallInfo,
    argument: FirExpression,
    parameter: FirValueParameter?,
): ConeKotlinType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedType(context.session, parameter)

    val expectedType =
        getExpectedTypeWithSAMConversion(argument, basicExpectedType)?.also {
            context.session.lookupTracker?.let { lookupTracker ->
                parameter.returnTypeRef.coneType.classLikeLookupTagIfAny?.takeIf {
                    it.toSymbol()?.isLocal == false
                }?.let { lookupTag ->
                    val classId = lookupTag.classId
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
            ?: getExpectedTypeWithImplicitIntegerCoercion(context.session, argument, parameter, basicExpectedType)
            ?: basicExpectedType
    return this.substitutor.substituteOrSelf(expectedType)
}

context(context: ResolutionContext)
private fun Candidate.getExpectedTypeWithSAMConversion(
    argument: FirExpression,
    candidateExpectedType: ConeKotlinType,
): ConeKotlinType? {
    if (candidateExpectedType.isSomeFunctionType(context.session)) return null

    val samConversionInfo =
        context.bodyResolveComponents.samResolver.getSamInfoForPossibleSamType(candidateExpectedType)
            ?: return null

    if (!argument.shouldUseSamConversion(candidate = this, candidateExpectedType)) {
        return null
    }

    setSamConversionOfArgument(argument.unwrapArgument(), samConversionInfo)
    return samConversionInfo.functionalType
}

context(context: ResolutionContext)
private fun FirExpression.shouldUseSamConversion(
    candidate: Candidate,
    candidateExpectedType: ConeKotlinType,
): Boolean {
    val unwrapped = unwrapArgument()

    // Always apply SAM conversion on lambdas and callable references
    if (unwrapped is FirAnonymousFunctionExpression || unwrapped is FirCallableReferenceAccess) {
        return true
    }

    // Always apply SAM conversion on generic call with nested lambda like `run { {} }`
    if (unwrapped.isCallWithGenericReturnTypeAndMatchingLambda()) {
        return true
    }

    val expressionType = resolvedType
    // Expression type is a subtype of expected type, no need for SAM conversion.
    val substitutedExpectedType = candidate.substitutor.substituteOrSelf(candidateExpectedType)
    if (candidate.csBuilder.isSubtypeConstraintCompatible(expressionType, substitutedExpectedType)) {
        return false
    }

    // After the `if` above, we know that the argument type is anyway not a subtype of the original SAM type,
    // thus if we proceed without the conversion, it would be an INAPPLICABLE candidate anyway.
    // So we might return `true` here unconditionally, but we check if there's some hope for proper SAM conversion
    // to report better diagnostics otherwise.
    return context(context.typeContext) {
        expressionType.anySuperTypeConstructor {
            require(it is ConeKotlinType)
            it.isSomeFunctionType(context.session)
        }
    }
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
context(context: SessionHolder)
private fun FirExpression.isCallWithGenericReturnTypeAndMatchingLambda(): Boolean {
    val session = context.session
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
    candidateExpectedType: ConeKotlinType,
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

context(context: ResolutionContext)
private fun CheckerSink.markCandidateForCompatibilityResolve() {
    if (disableCompatibilityModeForNewInference()) return
    reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
}
