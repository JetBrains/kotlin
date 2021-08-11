/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.commonSuperType
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.ComposedSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.calls.util.extractCallableReferenceExpression
import org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver
) {
    private val topLevelCallCheckerContext = CallCheckerContext(
        topLevelCallContext, deprecationResolver, moduleDescriptor, missingSupertypesResolver
    )
    private val topLevelTrace = topLevelCallCheckerContext.trace

    private data class CallableReferenceResultTypeInfo(
        val dispatchReceiver: ReceiverValue?,
        val extensionReceiver: ReceiverValue?,
        val explicitReceiver: ReceiverValue?,
        val substitutor: NewTypeSubstitutor,
        val resultType: KotlinType
    )

    private fun complete(resolvedAtom: ResolvedAtom) {
        if (topLevelCallContext.inferenceSession.callCompleted(resolvedAtom)) {
            return
        }

        when (resolvedAtom) {
            is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceArgumentAtom -> completeCallableReferenceArgument(resolvedAtom)
            is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
            is ResolvedSubCallArgument -> completeSubCallArgument(resolvedAtom)
            is ResolvedExpressionAtom -> completeExpression(resolvedAtom)
            else -> {}
        }
    }

    // We run completion on expressions only for last statements of block expression to substitute freshly inferred stub types variables
    fun completeExpression(resolvedAtom: ResolvedExpressionAtom) {
        val argumentExpression = resolvedAtom.atom.psiExpression
        val inferenceSession = topLevelCallContext.inferenceSession

        if (argumentExpression !is KtBlockExpression || inferenceSession !is BuilderInferenceSession) return

        val callableReference = argumentExpression.statements.lastOrNull() as? KtCallableReferenceExpression ?: return

        inferenceSession.completeDoubleColonExpression(callableReference, inferenceSession.getNotFixedToInferredTypesSubstitutor())
    }

    fun completeAll(resolvedAtom: ResolvedAtom) {
        if (!resolvedAtom.analyzed)
            return
        resolvedAtom.subResolvedAtoms?.forEach { subKtPrimitive ->
            completeAll(subKtPrimitive)
        }
        complete(resolvedAtom)
    }

    private fun completeSubCallArgument(resolvedSubCallArgument: ResolvedSubCallArgument) {
        val deparenthesizedExpression =
            KtPsiUtil.getLastElementDeparenthesized(resolvedSubCallArgument.atom.psiExpression, topLevelCallContext.statementFilter)

        if (deparenthesizedExpression is KtCallableReferenceExpression) {
            val callableReferenceResolvedCall = kotlinToResolvedCallTransformer.getResolvedCallForArgumentExpression(
                deparenthesizedExpression.callableReference, topLevelCallContext
            ) as? NewCallableReferenceResolvedCall<*>
            if (callableReferenceResolvedCall != null) {
                completeCallableReferenceCall(callableReferenceResolvedCall)
            }
        } else {
            kotlinToResolvedCallTransformer.updateRecordedType(
                resolvedSubCallArgument.atom.psiExpression ?: return,
                parameter = null,
                context = topLevelCallContext.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE),
                reportErrorForTypeMismatch = true,
                convertedArgumentType = null
            )
        }
    }

    fun completeResolvedCall(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<KotlinCallDiagnostic>
    ): NewAbstractResolvedCall<*>? {
        val diagnosticsFromPartiallyResolvedCall = extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom)

        clearPartiallyResolvedCall(resolvedCallAtom)

        val atom = resolvedCallAtom.atom
        if (atom.psiKotlinCall is PSIKotlinCallForVariable) return null

        val allDiagnostics = diagnostics + diagnosticsFromPartiallyResolvedCall

        val resolvedCall = kotlinToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            allDiagnostics
        )

        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) {
            resolvedCall.functionCall as NewAbstractResolvedCall<*>
        } else resolvedCall
        if (ErrorUtils.isError(resolvedCall.candidateDescriptor)) {
            kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
            checkMissingReceiverSupertypes(resolvedCall, missingSupertypesResolver, topLevelTrace)
            return resolvedCall
        }

        val psiCallForResolutionContext = when (atom) {
            // PARTIAL_CALL_RESOLUTION_CONTEXT has been written for the baseCall
            is PSIKotlinCallForInvoke -> atom.baseCall.psiCall
            else -> atom.psiKotlinCall.psiCall
        }

        val callElement = psiCallForResolutionContext.callElement
        if (callElement is KtExpression) {
            val recordedType = topLevelCallContext.trace.getType(callElement)
            if (recordedType != null && recordedType.shouldBeUpdated() && resolvedCall.resultingDescriptor.returnType != null) {
                topLevelCallContext.trace.recordType(callElement, resolvedCall.resultingDescriptor.returnType)
            }
        }

        val resolutionContextForPartialCall =
            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCallForResolutionContext]

        val callCheckerContext = if (resolutionContextForPartialCall != null)
            CallCheckerContext(
                resolutionContextForPartialCall.replaceBindingTrace(topLevelTrace),
                deprecationResolver,
                moduleDescriptor,
                missingSupertypesResolver
            )
        else
            topLevelCallCheckerContext

        kotlinToResolvedCallTransformer.bind(topLevelTrace, resolvedCall)

        kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
        kotlinToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)
        kotlinToResolvedCallTransformer.runAdditionalReceiversCheckers(resolvedCall, topLevelCallContext)

        kotlinToResolvedCallTransformer.reportDiagnostics(topLevelCallContext, topLevelTrace, resolvedCall, allDiagnostics)

        return resolvedCall
    }

    private fun checkMissingReceiverSupertypes(
        resolvedCall: ResolvedCall<CallableDescriptor>,
        missingSupertypesResolver: MissingSupertypesResolver,
        trace: BindingTrace
    ) {
        val receiverValue = resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver
        receiverValue?.type?.let { receiverType ->
            MissingDependencySupertypeChecker.checkSupertypes(
                receiverType,
                resolvedCall.call.callElement,
                trace,
                missingSupertypesResolver
            )
        }
    }

    private fun extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Set<KotlinCallDiagnostic> {
        val psiCall = KotlinToResolvedCallTransformer.keyForPartiallyResolvedCall(resolvedCallAtom)
        val partialCallContainer = topLevelTrace[BindingContext.ONLY_RESOLVED_CALL, psiCall]

        return partialCallContainer?.result?.diagnostics.orEmpty().toSet()
    }

    private fun clearPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom) {
        val psiCall = KotlinToResolvedCallTransformer.keyForPartiallyResolvedCall(resolvedCallAtom)

        val partialCallContainer = topLevelTrace[BindingContext.ONLY_RESOLVED_CALL, psiCall]
        if (partialCallContainer != null) {
            topLevelTrace.record(BindingContext.ONLY_RESOLVED_CALL, psiCall, PartialCallContainer.empty)
        }
    }

    private val ResolvedLambdaAtom.isCoercedToUnit: Boolean
        get() {
            val resultArgumentsInfo = this.resultArgumentsInfo
                ?: return (subResolvedAtoms!!.single() as ResolvedLambdaAtom).isCoercedToUnit
            val returnTypes =
                resultArgumentsInfo.nonErrorArguments.map {
                    val type = it.safeAs<SimpleKotlinCallArgument>()?.receiver?.receiverValue?.type ?: return@map null
                    val unwrappedType = when (type) {
                        is WrappedType -> type.unwrap()
                        is UnwrappedType -> type
                    }
                    resultSubstitutor.safeSubstitute(unwrappedType)
                }
            if (returnTypes.isEmpty() && !resultArgumentsInfo.returnArgumentsExist) return true
            val substitutedTypes = returnTypes.filterNotNull()
            // we have some unsubstituted types
            if (substitutedTypes.isEmpty()) return false
            val commonReturnType = NewCommonSuperTypeCalculator.commonSuperType(substitutedTypes)
            return commonReturnType.isUnit()
        }

    private fun KotlinType.substituteAndApproximate(substitutor: NewTypeSubstitutor): FunctionLiteralTypes.ProcessedType {
        val substitutedType = substitutor.safeSubstitute(this.unwrap())

        return FunctionLiteralTypes.ProcessedType(
            substitutedType,
            approximatedType = typeApproximator.approximateDeclarationType(
                substitutedType, local = true
            )
        )
    }

    fun substituteFunctionLiteralDescriptor(
        resolvedAtom: ResolvedLambdaAtom?, // null is for callable references resolved though the old type inference
        descriptor: SimpleFunctionDescriptorImpl,
        substitutor: NewTypeSubstitutor
    ): FunctionLiteralTypes {
        val returnType =
            (if (resolvedAtom?.isCoercedToUnit == true) builtIns.unitType else resolvedAtom?.returnType) ?: descriptor.returnType
        val extensionReceiverType = resolvedAtom?.receiver ?: descriptor.extensionReceiverParameter?.type
        val dispatchReceiverType = descriptor.dispatchReceiverParameter?.type
        val valueParameterTypes = resolvedAtom?.parameters ?: descriptor.valueParameters.map { it.type }

        require(returnType != null)

        val substitutedReturnType = returnType.substituteAndApproximate(substitutor).also {
            descriptor.setReturnType(it.approximatedType)
        }

        val extensionReceiverFromDescriptor = descriptor.extensionReceiverParameter
        val substitutedReceiverType = extensionReceiverType?.substituteAndApproximate(substitutor)?.also {
            if (extensionReceiverFromDescriptor is ReceiverParameterDescriptorImpl && extensionReceiverFromDescriptor.type.shouldBeUpdated()) {
                extensionReceiverFromDescriptor.setOutType(it.approximatedType)
            }
        }

        val dispatchReceiverFromDescriptor = descriptor.dispatchReceiverParameter
        dispatchReceiverType?.substituteAndApproximate(substitutor)?.also {
            if (dispatchReceiverFromDescriptor is ReceiverParameterDescriptorImpl && dispatchReceiverFromDescriptor.type.shouldBeUpdated()) {
                dispatchReceiverFromDescriptor.setOutType(it.approximatedType)
            }
        }

        val substitutedValueParameterTypes = descriptor.valueParameters.mapIndexedNotNull { i, valueParameter ->
            valueParameterTypes.getOrNull(i)?.substituteAndApproximate(substitutor)?.also {
                if (valueParameter is ValueParameterDescriptorImpl && valueParameter.type.shouldBeUpdated()) {
                    valueParameter.setOutType(it.approximatedType)
                }
            }
        }

        return FunctionLiteralTypes(substitutedReturnType, substitutedValueParameterTypes, substitutedReceiverType)
    }

    private fun completeLambda(resolvedAtom: ResolvedLambdaAtom) {
        val lambda = resolvedAtom.unwrap()
        val resultArgumentsInfo = lambda.resultArgumentsInfo!!

        val psiCallArgument = lambda.atom.psiCallArgument
        val (ktArgumentExpression, ktFunction) = when (psiCallArgument) {
            is LambdaKotlinCallArgumentImpl -> psiCallArgument.ktLambdaExpression to psiCallArgument.ktLambdaExpression.functionLiteral
            is FunctionExpressionImpl -> psiCallArgument.ktFunction to psiCallArgument.ktFunction
            else -> throw AssertionError("Unexpected psiCallArgument for resolved lambda argument: $psiCallArgument")
        }

        val descriptor = topLevelTrace.bindingContext.get(BindingContext.FUNCTION, ktFunction) as? SimpleFunctionDescriptorImpl
            ?: throw AssertionError("No function descriptor for resolved lambda argument")

        val substitutedLambdaTypes = substituteFunctionLiteralDescriptor(lambda, descriptor, resultSubstitutor)

        val existingLambdaType = topLevelTrace.getType(ktArgumentExpression)

        if (existingLambdaType == null) {
            if (ktFunction is KtNamedFunction && ktFunction.nameIdentifier != null) return // it's a statement
            throw AssertionError("No type for resolved lambda argument: ${ktArgumentExpression.text}")
        }

        val substitutedFunctionalType = createFunctionType(
            builtIns,
            existingLambdaType.annotations,
            substitutedLambdaTypes.receiverType?.substitutedType,
            emptyList(),
            substitutedLambdaTypes.parameterTypes.map { it.substitutedType },
            null, // parameter names transforms to special annotations, so they are already taken from parameter types
            substitutedLambdaTypes.returnType.substitutedType,
            lambda.isSuspend
        )

        topLevelTrace.recordType(ktArgumentExpression, substitutedFunctionalType)

        for (lambdaResult in resultArgumentsInfo.nonErrorArguments) {
            val resultValueArgument = lambdaResult as? PSIKotlinCallArgument ?: continue
            val newContext = topLevelCallContext.replaceDataFlowInfo(resultValueArgument.dataFlowInfoAfterThisArgument)
                .replaceExpectedType(substitutedLambdaTypes.returnType.approximatedType)
                .replaceBindingTrace(topLevelTrace)
            val argumentExpression = resultValueArgument.valueArgument.getArgumentExpression() ?: continue

            kotlinToResolvedCallTransformer.updateRecordedType(
                argumentExpression,
                parameter = null,
                context = newContext,
                reportErrorForTypeMismatch = true,
                convertedArgumentType = null
            )
        }
    }

    private fun updateCallableReferenceResultType(callableCandidate: CallableReferenceResolutionCandidate): CallableReferenceResultTypeInfo? {
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiKotlinCall.psiCall.callElement.parent as? KtCallableReferenceExpression ?: return null
        val freshSubstitutor = callableCandidate.freshVariablesSubstitutor ?: return null
        val resultTypeParameters = freshSubstitutor.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }
        val typeParametersSubstitutor = NewTypeSubstitutorByConstructorMap(
            callableCandidate.candidate.typeParameters.map { it.typeConstructor }.zip(resultTypeParameters).toMap()
        )
        val resultSubstitutor = if (callableCandidate.candidate.isSupportedForCallableReference()) {
            ComposedSubstitutor(typeParametersSubstitutor, resultSubstitutor)
        } else EmptySubstitutor

        // write down type for callable reference expression
        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType)

        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
            topLevelTrace, expressionTypingServices.statementFilter, resultType, callableReferenceExpression
        )

        val dispatchReceiver = callableCandidate.dispatchReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
        val extensionReceiver = callableCandidate.extensionReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

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

        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(
            callableCandidate.candidate,
            topLevelCallContext.trace,
            callableReferenceExpression
        )

        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> callableCandidate.extensionReceiver
            else -> null
        }
        val explicitReceiver = explicitCallableReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        return CallableReferenceResultTypeInfo(dispatchReceiver, extensionReceiver, explicitReceiver, resultSubstitutor, resultType)
    }

    private fun extractCallableReferenceResultTypeInfoFromDescriptor(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor
    ): CallableReferenceResultTypeInfo {
        val dispatchReceiver = recordedDescriptor.dispatchReceiverParameter?.value
            ?: callableCandidate.dispatchReceiver?.receiver?.receiverValue
        val extensionReceiver = recordedDescriptor.extensionReceiverParameter?.value
            ?: callableCandidate.extensionReceiver?.receiver?.receiverValue
        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> extensionReceiver
            else -> null
        }

        return CallableReferenceResultTypeInfo(
            dispatchReceiver,
            extensionReceiver,
            explicitCallableReceiver,
            EmptySubstitutor,
            callableCandidate.reflectionCandidateType.replaceFunctionTypeArgumentsByDescriptor(recordedDescriptor)
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun KotlinType.replaceFunctionTypeArgumentsByDescriptor(descriptor: CallableDescriptor) =
        when (descriptor) {
            is CallableMemberDescriptor -> {
                val newArgumentTypes = buildList {
                    descriptor.extensionReceiverParameter?.let { add(it.type) }
                    addAll(descriptor.valueParameters.map { it.type })
                    add(descriptor.returnType)
                }
                if (newArgumentTypes.size == arguments.size) {
                    replace(arguments.mapIndexed { i, type -> newArgumentTypes[i]?.let { type.replaceType(it) } ?: type })
                } else this
            }
            is ValueDescriptor -> replace(descriptor.type.arguments)
            else -> this
        }

    fun completeCallableReferenceCall(resolvedCall: NewCallableReferenceResolvedCall<*>): KotlinType? {
        val candidate = resolvedCall.resolvedCallAtom?.candidate ?: return null
        return completeCallableReference(candidate, resolvedCall.resultingDescriptor, resolvedCall)
    }

    fun completeCallableReferenceArgument(resolvedAtom: ResolvedCallableReferenceArgumentAtom): KotlinType? {
        if (resolvedAtom.completed) return null

        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceKotlinCallArgumentImpl
        val callableReferenceCallCandidate = resolvedAtom.candidate ?: return null
        val descriptor = when (callableReferenceCallCandidate.candidate) {
            is FunctionDescriptor -> topLevelCallContext.trace.get(BindingContext.FUNCTION, psiCallArgument.ktCallableReferenceExpression)
            is PropertyDescriptor -> topLevelCallContext.trace.get(BindingContext.VARIABLE, psiCallArgument.ktCallableReferenceExpression)
            else -> null
        }
        val dataFlowInfo = resolvedAtom.atom.psiCallArgument.dataFlowInfoAfterThisArgument
        val resolvedCall = NewCallableReferenceResolvedCall<CallableDescriptor>(
            resolvedAtom,
            typeApproximator,
            expressionTypingServices.languageVersionSettings
        )

        return completeCallableReference(callableReferenceCallCandidate, descriptor, resolvedCall, dataFlowInfo)
            .also { resolvedAtom.completed = true }
    }

    private fun completeCallableReference(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor?,
        resolvedCall: NewAbstractResolvedCall<*>,
        additionalDataFlowInfo: DataFlowInfo? = null,
    ): KotlinType? {
        val rawExtensionReceiver = callableCandidate.extensionReceiver
        val unrestrictedBuilderInferenceSupported =
            topLevelCallContext.languageVersionSettings.supportsFeature(LanguageFeature.UnrestrictedBuilderInference)
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiKotlinCall.extractCallableReferenceExpression() ?: return null

        if (rawExtensionReceiver != null && !unrestrictedBuilderInferenceSupported && rawExtensionReceiver.receiver.receiverValue.type.contains { it is StubTypeForBuilderInference }) {
            topLevelTrace.reportDiagnosticOnce(Errors.TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE.on(callableReferenceExpression))
            return null
        }

        val resultTypeInfo = if (recordedDescriptor != null) {
            extractCallableReferenceResultTypeInfoFromDescriptor(callableCandidate, recordedDescriptor)
        } else {
            updateCallableReferenceResultType(callableCandidate)
        }

        if (resultTypeInfo == null) return null

        resolvedCall.apply {
            if (resultTypeInfo.dispatchReceiver != null) {
                updateDispatchReceiverType(resultTypeInfo.dispatchReceiver.type)
            }
            if (resultTypeInfo.extensionReceiver != null) {
                updateExtensionReceiverType(resultTypeInfo.extensionReceiver.type)
            }
            setResultingSubstitutor(resultTypeInfo.substitutor)
        }

        recordArgumentAdaptationForCallableReference(resolvedCall, callableCandidate.callableReferenceAdaptation)

        val psiCall = CallMaker.makeCall(
            callableReferenceExpression.callableReference,
            resultTypeInfo.explicitReceiver,
            null,
            callableReferenceExpression.callableReference,
            emptyList()
        )
        val tracing = TracingStrategyImpl.create(callableReferenceExpression.callableReference, psiCall)

        tracing.bindCall(topLevelTrace, psiCall)
        tracing.bindReference(topLevelTrace, resolvedCall)
        tracing.bindResolvedCall(topLevelTrace, resolvedCall)

        // TODO: probably we should also record key 'DATA_FLOW_INFO_BEFORE', see ExpressionTypingVisitorDispatcher.getTypeInfo
        val typeInfo = if (additionalDataFlowInfo != null) {
            createTypeInfo(resultTypeInfo.resultType, additionalDataFlowInfo)
        } else {
            createTypeInfo(resultTypeInfo.resultType)
        }

        topLevelTrace.record(BindingContext.EXPRESSION_TYPE_INFO, callableReferenceExpression, typeInfo)
        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)

        kotlinToResolvedCallTransformer.runCallCheckers(resolvedCall, topLevelCallCheckerContext)

        return resultTypeInfo.resultType
    }

    private fun ReceiverValue.updateReceiverValue(substitutor: NewTypeSubstitutor): ReceiverValue {
        val newType = substitutor.safeSubstitute(type.unwrap()).let {
            typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference) ?: it
        }
        return if (type != newType) replaceType(newType as KotlinType) else this
    }

    private fun recordArgumentAdaptationForCallableReference(
        resolvedCall: NewAbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation?
    ) {
        if (callableReferenceAdaptation == null) return

        val callElement = resolvedCall.call.callElement
        val isUnboundReference = resolvedCall.dispatchReceiver is TransientReceiver

        fun makeFakeValueArgument(callArgument: KotlinCallArgument): ValueArgument {
            val fakeCallArgument = callArgument as? FakeKotlinCallArgumentForCallableReference
                ?: throw AssertionError("FakeKotlinCallArgumentForCallableReference expected: $callArgument")
            return FakePositionalValueArgumentForCallableReferenceImpl(
                callElement,
                if (isUnboundReference) fakeCallArgument.index + 1 else fakeCallArgument.index
            )
        }

        // We should record argument mapping only if callable reference requires adaptation:
        // - argument mapping is non-trivial: any of the arguments were mapped as defaults or vararg elements;
        // - result should be coerced.
        var hasNonTrivialMapping = false
        val mappedArguments = ArrayList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>()
        for ((valueParameter, resolvedCallArgument) in callableReferenceAdaptation.mappedArguments) {
            val resolvedValueArgument = when (resolvedCallArgument) {
                ResolvedCallArgument.DefaultArgument -> {
                    hasNonTrivialMapping = true
                    DefaultValueArgument.DEFAULT
                }
                is ResolvedCallArgument.SimpleArgument -> {
                    val valueArgument = makeFakeValueArgument(resolvedCallArgument.callArgument)
                    if (valueParameter.isVararg)
                        VarargValueArgument(
                            listOf(
                                FakeImplicitSpreadValueArgumentForCallableReferenceImpl(callElement, valueArgument)
                            )
                        )
                    else
                        ExpressionValueArgument(valueArgument)
                }
                is ResolvedCallArgument.VarargArgument -> {
                    hasNonTrivialMapping = true
                    VarargValueArgument(
                        resolvedCallArgument.arguments.map {
                            makeFakeValueArgument(it)
                        }
                    )
                }
            }
            mappedArguments.add(valueParameter to resolvedValueArgument)
        }
        if (hasNonTrivialMapping || isCallableReferenceWithImplicitConversion(resolvedCall, callableReferenceAdaptation)) {
            resolvedCall.updateValueArguments(mappedArguments.toMap())
        }
    }

    private fun isCallableReferenceWithImplicitConversion(
        resolvedCall: NewAbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation
    ): Boolean {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        // TODO drop return type check - see noCoercionToUnitIfFunctionAlreadyReturnsUnit.kt
        if (callableReferenceAdaptation.coercionStrategy == CoercionStrategy.COERCION_TO_UNIT && !resultingDescriptor.returnType!!.isUnit())
            return true

        if (callableReferenceAdaptation.suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION)
            return true

        return false
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

class FunctionLiteralTypes(
    val returnType: ProcessedType,
    val parameterTypes: List<ProcessedType>,
    val receiverType: ProcessedType?
) {
    class ProcessedType(val substitutedType: KotlinType, val approximatedType: KotlinType)
}
