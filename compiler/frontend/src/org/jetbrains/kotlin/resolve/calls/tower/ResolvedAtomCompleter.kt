/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DeprecationResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.types.IndexedParametersSubstitution
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class ResolvedAtomCompleter(
    private val resultSubstitutor: NewTypeSubstitutor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val builtIns: KotlinBuiltIns,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    private val topLevelCallCheckerContext = CallCheckerContext(topLevelCallContext, deprecationResolver, moduleDescriptor)
    private val topLevelTrace = topLevelCallCheckerContext.trace

    private fun complete(resolvedAtom: ResolvedAtom) {
        when (resolvedAtom) {
            is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceAtom -> completeCallableReference(resolvedAtom)
            is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
            is PartialCallResolutionResult -> completeResolvedCall(resolvedAtom.resultCallAtom, resolvedAtom.diagnostics)
        }
    }

    fun completeAll(resolvedAtom: ResolvedAtom) {
        for (subKtPrimitive in resolvedAtom.subResolvedAtoms) {
            completeAll(subKtPrimitive)
        }
        complete(resolvedAtom)
    }

    fun completeResolvedCall(resolvedCallAtom: ResolvedCallAtom, diagnostics: Collection<KotlinCallDiagnostic>): ResolvedCall<*>? {
        if (resolvedCallAtom.atom.psiKotlinCall is PSIKotlinCallForVariable) return null

        val resolvedCall = kotlinToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            diagnostics
        )

        val resolutionContextForPartialCall =
            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, resolvedCallAtom.atom.psiKotlinCall.psiCall]

        val callCheckerContext = if (resolutionContextForPartialCall != null)
            CallCheckerContext(
                resolutionContextForPartialCall.replaceBindingTrace(topLevelTrace),
                deprecationResolver,
                moduleDescriptor
            )
        else
            topLevelCallCheckerContext

        kotlinToResolvedCallTransformer.bindAndReport(topLevelCallContext, topLevelTrace, resolvedCall, diagnostics)
        kotlinToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)

        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) resolvedCall.functionCall else resolvedCall
        kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, topLevelTrace, lastCall as NewResolvedCallImpl<*>)

        return resolvedCall
    }

    private fun completeLambda(lambda: ResolvedLambdaAtom) {
        val returnType = resultSubstitutor.safeSubstitute(lambda.returnType)

        updateTraceForLambda(lambda, topLevelTrace, returnType)

        for (lambdaResult in lambda.resultArguments) {
            val resultValueArgument = lambdaResult as? PSIKotlinCallArgument ?: continue
            val newContext =
                topLevelCallContext.replaceDataFlowInfo(resultValueArgument.dataFlowInfoAfterThisArgument)
                    .replaceExpectedType(returnType)
                    .replaceBindingTrace(topLevelTrace)

            val argumentExpression = resultValueArgument.valueArgument.getArgumentExpression() ?: continue
            kotlinToResolvedCallTransformer.updateRecordedType(argumentExpression, newContext, true)
        }
    }

    private fun updateTraceForLambda(lambda: ResolvedLambdaAtom, trace: BindingTrace, returnType: UnwrappedType) {
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

        val functionDescriptor = trace.bindingContext.get(BindingContext.FUNCTION, ktFunction) as? FunctionDescriptorImpl
                ?: throw AssertionError("No function descriptor for resolved lambda argument")
        functionDescriptor.setReturnType(returnType)

        val existingLambdaType = trace.getType(ktArgumentExpression)
        if (existingLambdaType == null) {
            if (ktFunction is KtNamedFunction && ktFunction.nameIdentifier != null) return // it's a statement

            throw AssertionError("No type for resolved lambda argument: ${ktArgumentExpression.text}")
        }
        val substitutedFunctionalType = createFunctionType(
            builtIns,
            existingLambdaType.annotations,
            lambda.receiver?.let { resultSubstitutor.substituteKeepAnnotations(it) },
            lambda.parameters.map { resultSubstitutor.substituteKeepAnnotations(it) },
            null, // parameter names transforms to special annotations, so they are already taken from parameter types
            returnType,
            lambda.isSuspend
        )

        trace.recordType(ktArgumentExpression, substitutedFunctionalType)

        // Mainly this is needed for builder-like inference, when we have type `SomeType<K, V>.() -> Unit` and now we want to update those K, V
        val receiver = functionDescriptor.extensionReceiverParameter
        if (receiver != null) {
            require(receiver is ReceiverParameterDescriptorImpl) {
                "Extension receiver for anonymous function ($receiver) should be ReceiverParameterDescriptorImpl"
            }

            val valueType = receiver.value.type.unwrap()
            val newValueType = resultSubstitutor.substituteKeepAnnotations(valueType)

            val newReceiverValue = receiver.value.replaceType(newValueType)

            functionDescriptor.setExtensionReceiverParameter(
                ReceiverParameterDescriptorImpl(receiver.containingDeclaration, newReceiverValue, receiver.annotations)
            )
        }
    }

    private fun completeCallableReference(
        resolvedAtom: ResolvedCallableReferenceAtom
    ) {
        val callableCandidate = resolvedAtom.candidate
        if (callableCandidate == null) {
            // todo report meanfull diagnostic here
            return
        }
        val resultTypeParameters =
            callableCandidate.freshSubstitutor!!.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }


        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceKotlinCallArgumentImpl
        val callableReferenceExpression = psiCallArgument.ktCallableReferenceExpression
        val resultSubstitutor = IndexedParametersSubstitution(
            callableCandidate.candidate.typeParameters,
            resultTypeParameters.map { it.asTypeProjection() }).buildSubstitutor()


        // write down type for callable reference expression
        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType, Variance.INVARIANT)
        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
            topLevelTrace, expressionTypingServices.statementFilter,
            resultType,
            callableReferenceExpression
        )
        val reference = callableReferenceExpression.callableReference

        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> callableCandidate.extensionReceiver
            else -> null
        }

        val explicitReceiver = explicitCallableReceiver?.receiver
        val psiCall = CallMaker.makeCall(reference, explicitReceiver?.receiverValue, null, reference, emptyList())

        val tracing = TracingStrategyImpl.create(reference, psiCall)
        val temporaryTrace = TemporaryBindingTrace.create(topLevelTrace, "callable reference fake call")

        val resolvedCall = ResolvedCallImpl(
            psiCall, callableCandidate.candidate, callableCandidate.dispatchReceiver?.receiver?.receiverValue,
            callableCandidate.extensionReceiver?.receiver?.receiverValue, callableCandidate.explicitReceiverKind,
            null, temporaryTrace, tracing, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
        )
        resolvedCall.setResultingSubstitutor(resultSubstitutor)

        tracing.bindCall(topLevelTrace, psiCall)
        tracing.bindReference(topLevelTrace, resolvedCall)
        tracing.bindResolvedCall(topLevelTrace, resolvedCall)

        resolvedCall.setStatusToSuccess()
        resolvedCall.markCallAsCompleted()

        when (callableCandidate.candidate) {
            is FunctionDescriptor -> doubleColonExpressionResolver.bindFunctionReference(
                callableReferenceExpression,
                resultType,
                topLevelCallContext,
                callableCandidate.candidate as FunctionDescriptor
            )
            is PropertyDescriptor -> doubleColonExpressionResolver.bindPropertyReference(
                callableReferenceExpression,
                resultType,
                topLevelCallContext
            )
        }

        // TODO: probably we should also record key 'DATA_FLOW_INFO_BEFORE', see ExpressionTypingVisitorDispatcher.getTypeInfo
        topLevelTrace.recordType(callableReferenceExpression, resultType)
        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)

        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(
            callableCandidate.candidate,
            topLevelCallContext.trace,
            callableReferenceExpression
        )
    }

    private fun completeCollectionLiteralCalls(collectionLiteralArgument: ResolvedCollectionLiteralAtom) {
        val psiCallArgument = collectionLiteralArgument.atom.psiCallArgument as CollectionLiteralKotlinCallArgumentImpl
        val context = psiCallArgument.outerCallContext

        val expectedType =
            collectionLiteralArgument.expectedType?.let { resultSubstitutor.safeSubstitute(it) } ?: TypeUtils.NO_EXPECTED_TYPE

        val actualContext = context
            .replaceBindingTrace(topLevelTrace)
            .replaceExpectedType(expectedType)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        expressionTypingServices.getTypeInfo(psiCallArgument.collectionLiteralExpression, actualContext)
    }
}
