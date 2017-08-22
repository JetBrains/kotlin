/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.replaceReturnType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ResolvedAtomCompleter(
        private val resultSubstitutor: NewTypeSubstitutor,
        private val trace: BindingTrace,
        private val topLevelCallContext: BasicCallResolutionContext,

        private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
        private val expressionTypingServices: ExpressionTypingServices,
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
        languageVersionSettings: LanguageVersionSettings
) {
    private val callCheckerContext = CallCheckerContext(topLevelCallContext, languageVersionSettings)

    fun completeAndReport(resolvedAtom: ResolvedAtom) {
        when (resolvedAtom) {
            is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceAtom -> completeCallableReference(resolvedAtom)
            is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom)
        }
    }

    fun completeAll(resolvedAtom: ResolvedAtom) {
        for (subKtPrimitive in resolvedAtom.subResolvedAtoms) {
            completeAll(subKtPrimitive)
        }
        completeAndReport(resolvedAtom)
    }

    fun completeResolvedCall(resolvedCallAtom: ResolvedCallAtom): ResolvedCall<*>? {
        if (resolvedCallAtom.atom.psiKotlinCall is PSIKotlinCallForVariable) return null

        val resolvedCall = kotlinToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(resolvedCallAtom, trace, resultSubstitutor)
        kotlinToResolvedCallTransformer.bindAndReport(topLevelCallContext, trace, resolvedCall)
        kotlinToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)

        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) resolvedCall.functionCall else resolvedCall
        kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, trace, lastCall as NewResolvedCallImpl<*>)

        return resolvedCall
    }

    private fun completeLambda(lambda: ResolvedLambdaAtom) {
            val returnType = resultSubstitutor.safeSubstitute(lambda.returnType)

            updateTraceForLambdaReturnType(lambda, trace, returnType)

            for (lambdaResult in lambda.resultArguments) {
                val resultValueArgument = lambdaResult as? PSIKotlinCallArgument ?: continue
                val newContext =
                        topLevelCallContext.replaceDataFlowInfo(resultValueArgument.dataFlowInfoAfterThisArgument)
                                .replaceExpectedType(returnType)
                                .replaceBindingTrace(trace)

                val argumentExpression = resultValueArgument.valueArgument.getArgumentExpression() ?: continue
                kotlinToResolvedCallTransformer.updateRecordedType(argumentExpression, newContext)
            }
    }

    private fun updateTraceForLambdaReturnType(lambda: ResolvedLambdaAtom, trace: BindingTrace, returnType: UnwrappedType) {
        val psiCallArgument = lambda.atom.psiCallArgument

        val ktArgumentExpression: KtExpression
        val ktFunction: KtElement
        when (psiCallArgument) {
            is LambdaKotlinCallArgumentImpl -> {
                ktArgumentExpression = psiCallArgument.ktLambdaExpression
                ktFunction = ktArgumentExpression.functionLiteral
            }
            is FunctionExpressionImpl -> {
                ktArgumentExpression = psiCallArgument.ktFunction
                ktFunction = ktArgumentExpression
            }
            else -> throw AssertionError("Unexpected psiCallArgument for resolved lambda argument: $psiCallArgument")
        }

        val functionDescriptor = trace.bindingContext.get(BindingContext.FUNCTION, ktFunction) as? FunctionDescriptorImpl ?:
                                 throw AssertionError("No function descriptor for resolved lambda argument")
        functionDescriptor.setReturnType(returnType)

        val existingLambdaType = trace.getType(ktArgumentExpression) ?: throw AssertionError("No type for resolved lambda argument")
        trace.recordType(ktArgumentExpression, existingLambdaType.replaceReturnType(returnType))
    }

    private fun completeCallableReference(
            resolvedAtom: ResolvedCallableReferenceAtom
    ) {
        val callableCandidate = resolvedAtom.candidate
        if (callableCandidate == null) {
            // todo report meanfull diagnostic here
            return
        }
        val resultTypeParameters = callableCandidate.freshSubstitutor!!.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }


        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceKotlinCallArgumentImpl
        val callableReferenceExpression = psiCallArgument.ktCallableReferenceExpression
        val resultSubstitutor = IndexedParametersSubstitution(callableCandidate.candidate.typeParameters, resultTypeParameters.map { it.asTypeProjection() }).buildSubstitutor()


        // write down type for callable reference expression
        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType, Variance.INVARIANT)
        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(trace, expressionTypingServices.statementFilter,
                                                                    resultType,
                                                                    callableReferenceExpression)
        val reference = callableReferenceExpression.callableReference

        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> callableCandidate.extensionReceiver
            else -> null
        }

        val explicitReceiver = explicitCallableReceiver?.receiver
        val psiCall = CallMaker.makeCall(reference, explicitReceiver?.receiverValue, null, reference, emptyList())

        val tracing = TracingStrategyImpl.create(reference, psiCall)
        val temporaryTrace = TemporaryBindingTrace.create(trace, "callable reference fake call")

        val resolvedCall = ResolvedCallImpl(psiCall, callableCandidate.candidate, callableCandidate.dispatchReceiver?.receiver?.receiverValue,
                                            callableCandidate.extensionReceiver?.receiver?.receiverValue, callableCandidate.explicitReceiverKind,
                                            null, temporaryTrace, tracing, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY))
        resolvedCall.setResultingSubstitutor(resultSubstitutor)

        tracing.bindCall(trace, psiCall)
        tracing.bindReference(trace, resolvedCall)
        tracing.bindResolvedCall(trace, resolvedCall)

        resolvedCall.setStatusToSuccess()
        resolvedCall.markCallAsCompleted()

        when (callableCandidate.candidate) {
            is FunctionDescriptor -> doubleColonExpressionResolver.bindFunctionReference(callableReferenceExpression, resultType, topLevelCallContext)
            is PropertyDescriptor -> doubleColonExpressionResolver.bindPropertyReference(callableReferenceExpression, resultType, topLevelCallContext)
        }

        // TODO: probably we should also record key 'DATA_FLOW_INFO_BEFORE', see ExpressionTypingVisitorDispatcher.getTypeInfo
        trace.recordType(callableReferenceExpression, resultType)
        trace.record(BindingContext.PROCESSED, callableReferenceExpression)

        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(callableCandidate.candidate, topLevelCallContext.trace, callableReferenceExpression)
    }

    private fun completeCollectionLiteralCalls(collectionLiteralArgument: ResolvedCollectionLiteralAtom) {
        val psiCallArgument = collectionLiteralArgument.atom.psiCallArgument as CollectionLiteralKotlinCallArgumentImpl
        val context = psiCallArgument.outerCallContext

        val expectedType = collectionLiteralArgument.expectedType?.let { resultSubstitutor.safeSubstitute(it) } ?: TypeUtils.NO_EXPECTED_TYPE

        val actualContext = context
                .replaceBindingTrace(trace)
                .replaceExpectedType(expectedType)
                .replaceContextDependency(ContextDependency.INDEPENDENT)

        expressionTypingServices.getTypeInfo(psiCallArgument.collectionLiteralExpression, actualContext)
    }
}