/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.KotlinCallResolver
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerWithAdditionalResolve
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.stableType
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.BuilderLambdaLabelingInfo
import org.jetbrains.kotlin.resolve.calls.util.replaceArguments
import org.jetbrains.kotlin.resolve.calls.util.replaceTypes
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor

class ResolutionWithStubTypesChecker(private val kotlinCallResolver: KotlinCallResolver) : CallCheckerWithAdditionalResolve {
    override fun check(
        overloadResolutionResults: OverloadResolutionResults<*>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        // Don't check builder inference lambdas if the entire builder call itself has resolution ambiguity
        if (!overloadResolutionResults.isSingleResult) return

        val builderResolvedCall = overloadResolutionResults.resultingCall as? NewAbstractResolvedCall<*> ?: return

        val builderLambdas = (builderResolvedCall.psiKotlinCall.argumentsInParenthesis + builderResolvedCall.psiKotlinCall.externalArgument)
            .filterIsInstance<LambdaKotlinCallArgument>()
            .filter { it.hasBuilderInferenceAnnotation }

        for (lambda in builderLambdas) {
            val builderInferenceSession = lambda.builderInferenceSession as? BuilderInferenceSession ?: continue
            val errorCalls = builderInferenceSession.errorCallsInfo
            for (errorCall in errorCalls) {
                val resolutionResult = errorCall.result
                if (resolutionResult.isAmbiguity) {
                    val firstResolvedCall = resolutionResult.resultingCalls.first() as? NewAbstractResolvedCall<*> ?: continue
                    processResolutionAmbiguityError(context, firstResolvedCall, lambda, resolutionCallbacks, expectedType, scopeTower)
                }
            }
        }
    }

    private fun processResolutionAmbiguityError(
        context: BasicCallResolutionContext,
        firstResolvedCall: NewAbstractResolvedCall<*>,
        lambda: LambdaKotlinCallArgument,
        resolutionCallbacks: KotlinResolutionCallbacks,
        expectedType: UnwrappedType?,
        scopeTower: ImplicitScopeTower,
    ) {
        val kotlinCall = firstResolvedCall.psiKotlinCall
        val calleeExpression = kotlinCall.psiCall.calleeExpression
        val builderCalleeExpression = context.call.calleeExpression

        if (calleeExpression == null || builderCalleeExpression == null) return

        val receiverValue = firstResolvedCall.extensionReceiver
        val valueArguments = kotlinCall.argumentsInParenthesis

        val builderInferenceSession = lambda.builderInferenceSession as BuilderInferenceSession
        val stubVariablesSubstitutor = builderInferenceSession.getNotFixedToInferredTypesSubstitutor()
        val variablesForUsedStubTypes = builderInferenceSession.getUsedStubTypes().map { it.originalTypeVariable }
        val typeVariablesSubstitutionMap = (builderInferenceSession.getCurrentSubstitutor() as NewTypeSubstitutorByConstructorMap).map
            .filterKeys { it in variablesForUsedStubTypes }

        val newReceiverArgument = receiverValue?.buildSubstitutedReceiverArgument(stubVariablesSubstitutor, context)
        val newArguments = valueArguments.replaceTypes(context, resolutionCallbacks) { _, type ->
            stubVariablesSubstitutor.safeSubstitute(type)
        }

        if (newReceiverArgument == null && valueArguments == newArguments) return

        val newCall = kotlinCall.replaceArguments(newArguments, newReceiverArgument)
        val candidatesForSubstitutedCall = kotlinCallResolver.resolveCall(
            scopeTower, resolutionCallbacks, newCall, expectedType, context.collectAllCandidates
        )

        // It means we can't disambiguate the call with substituted receiver and arguments
        if (candidatesForSubstitutedCall.size != 1) return

        val typeVariablesCausedAmbiguity = reportStubTypeCausesAmbiguityOnArgumentsIfNeeded(
            valueArguments, newArguments, context, typeVariablesSubstitutionMap
        ).toMutableSet()

        val newReceiverValue = newReceiverArgument?.receiverValue

        if (receiverValue != null && newReceiverValue != null) {
            typeVariablesCausedAmbiguity.addAll(
                reportStubTypeCausesAmbiguityOnReceiverIfNeeded(
                    receiverValue, newReceiverValue, kotlinCall, lambda, context, typeVariablesSubstitutionMap
                )
            )
        }

        if (typeVariablesCausedAmbiguity.isNotEmpty()) {
            context.trace.report(
                OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES.on(
                    calleeExpression,
                    builderCalleeExpression.toString(),
                    typeVariablesCausedAmbiguity.toString(),
                    calleeExpression.toString()
                )
            )
        }
    }

    private fun reportStubTypeCausesAmbiguityOnReceiverIfNeeded(
        receiver: ReceiverValue,
        newReceiver: ReceiverValue,
        kotlinCall: PSIKotlinCall,
        lambda: LambdaKotlinCallArgument,
        context: BasicCallResolutionContext,
        substitutionMap: Map<TypeConstructor, UnwrappedType>
    ): Set<NewTypeVariableConstructor> = buildSet {
        val receiverType = receiver.type
        val newReceiverType = newReceiver.type
        val relatedLambdaToLabel = (lambda.psiExpression as? KtLambdaExpression)?.takeIf {
            val lexicalScope = context.trace.bindingContext[BindingContext.LEXICAL_SCOPE, kotlinCall.psiCall.callElement]
            val nearestScopeDescriptor = lexicalScope?.ownerDescriptor
            // Don't need to store lambda psi element if it can be accessed though unmarked `this`
            nearestScopeDescriptor != null && nearestScopeDescriptor != (receiver as? ExtensionReceiver)?.declarationDescriptor
        }

        if (receiverType != newReceiverType) {
            val typeVariables = substitutionMap.map { it.key as NewTypeVariableConstructor }
            val typeParameters = typeVariables.joinToString { (it.originalTypeParameter?.name ?: it).toString() }
            val inferredTypes = substitutionMap.values.joinToString()

            addAll(typeVariables)

            context.trace.report(
                STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY.on(
                    kotlinCall.explicitReceiver?.psiExpression ?: kotlinCall.psiCall.callElement,
                    newReceiverType, typeParameters, inferredTypes,
                    if (relatedLambdaToLabel != null) BuilderLambdaLabelingInfo(relatedLambdaToLabel) else BuilderLambdaLabelingInfo.EMPTY
                )
            )
        }
    }

    private fun reportStubTypeCausesAmbiguityOnArgumentsIfNeeded(
        valueArguments: List<KotlinCallArgument>,
        newArguments: List<KotlinCallArgument>,
        context: BasicCallResolutionContext,
        substitutionMap: Map<TypeConstructor, UnwrappedType>
    ): Set<NewTypeVariableConstructor> = buildSet {
        for ((i, valueArgument) in valueArguments.withIndex()) {
            if (valueArgument !is SimpleKotlinCallArgument) continue

            val substitutedValueArgument = newArguments[i] as? SimpleKotlinCallArgument ?: continue
            val originalType = valueArgument.receiver.stableType
            val substitutedType = substitutedValueArgument.receiver.stableType

            if (originalType != substitutedType) {
                val psiExpression = valueArgument.psiExpression ?: continue
                val typeVariables = substitutionMap.map { it.key as NewTypeVariableConstructor }
                val typeParameters = typeVariables.joinToString { (it.originalTypeParameter?.name ?: it).toString() }
                val inferredTypes = substitutionMap.values.joinToString()

                addAll(typeVariables)

                context.trace.report(
                    STUB_TYPE_IN_ARGUMENT_CAUSES_AMBIGUITY.on(psiExpression, substitutedType, typeParameters, inferredTypes)
                )
            }
        }
    }

    private fun ReceiverValue.buildSubstitutedReceiverArgument(
        substitutor: NewTypeSubstitutor,
        context: BasicCallResolutionContext,
    ): ReceiverExpressionKotlinCallArgument? {
        val newType = substitutor.safeSubstitute(type.unwrap())
        val receiverValue = when (this) {
            is ExpressionReceiver -> ExpressionReceiver.create(expression, newType, context.trace.bindingContext)
            is ExtensionReceiver -> ExtensionReceiver(declarationDescriptor, newType, original)
            else -> return null
        }

        return ReceiverExpressionKotlinCallArgument(
            ReceiverValueWithSmartCastInfo(receiverValue, typesFromSmartCasts = emptySet(), true)
        )
    }
}
