/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.MissingSupertypesResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceAdaptation
import org.jetbrains.kotlin.resolve.calls.components.SuspendConversionStrategy
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.checkers.MissingDependencySupertypeChecker
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.CoercionStrategy
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isUnit
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

    private fun complete(resolvedAtom: ResolvedAtom) {
        if (topLevelCallContext.inferenceSession.callCompleted(resolvedAtom)) {
            return
        }

        when (resolvedAtom) {
            is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceAtom -> completeCallableReference(resolvedAtom)
            is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
            is ResolvedSubCallArgument -> completeSubCallArgument(resolvedAtom)
        }
    }

    fun completeAll(resolvedAtom: ResolvedAtom) {
        if (!resolvedAtom.analyzed)
            return
        resolvedAtom.subResolvedAtoms?.forEach { subKtPrimitive ->
            completeAll(subKtPrimitive)
        }
        complete(resolvedAtom)
    }

    fun completeSubCallArgument(resolvedSubCallArgument: ResolvedSubCallArgument) {
        val contextWithoutExpectedType = topLevelCallContext.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
        kotlinToResolvedCallTransformer.updateRecordedType(
            resolvedSubCallArgument.atom.psiExpression ?: return,
            parameter = null,
            context = contextWithoutExpectedType,
            reportErrorForTypeMismatch = true,
            convertedArgumentType = null
        )
    }

    fun completeResolvedCall(resolvedCallAtom: ResolvedCallAtom, diagnostics: Collection<KotlinCallDiagnostic>): ResolvedCall<*>? {
        val diagnosticsFromPartiallyResolvedCall = extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom)

        clearPartiallyResolvedCall(resolvedCallAtom)

        if (resolvedCallAtom.atom.psiKotlinCall is PSIKotlinCallForVariable) return null

        val allDiagnostics = diagnostics + diagnosticsFromPartiallyResolvedCall

        val resolvedCall = kotlinToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            allDiagnostics
        )

        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) resolvedCall.functionCall else resolvedCall
        if (ErrorUtils.isError(resolvedCall.candidateDescriptor)) {
            kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall as NewResolvedCallImpl<*>)
            checkMissingReceiverSupertypes(resolvedCall, missingSupertypesResolver, topLevelTrace)
            return resolvedCall
        }

        val resolutionContextForPartialCall =
            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, resolvedCallAtom.atom.psiKotlinCall.psiCall]

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

        kotlinToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall as NewResolvedCallImpl<*>)
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

    private fun completeLambda(lambda: ResolvedLambdaAtom) {
        val lambda = lambda.unwrap()
        val resultArgumentsInfo = lambda.resultArgumentsInfo!!
        val returnType = if (lambda.isCoercedToUnit) {
            builtIns.unitType
        } else {
            resultSubstitutor.safeSubstitute(lambda.returnType)
        }

        val approximatedReturnType =
            typeApproximator.approximateDeclarationType(
                returnType,
                local = true,
                languageVersionSettings = topLevelCallContext.languageVersionSettings
            )
        updateTraceForLambda(lambda, topLevelTrace, approximatedReturnType)

        for (lambdaResult in resultArgumentsInfo.nonErrorArguments) {
            val resultValueArgument = lambdaResult as? PSIKotlinCallArgument ?: continue
            val newContext =
                topLevelCallContext.replaceDataFlowInfo(resultValueArgument.dataFlowInfoAfterThisArgument)
                    .replaceExpectedType(approximatedReturnType)
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
            lambda.receiver?.let { resultSubstitutor.safeSubstitute(it) },
            lambda.parameters.map { resultSubstitutor.safeSubstitute(it) },
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
            val newValueType = resultSubstitutor.safeSubstitute(valueType)

            if (valueType !== newValueType) {
                val newReceiverValue = receiver.value.replaceType(newValueType)
                functionDescriptor.setExtensionReceiverParameter(
                    ReceiverParameterDescriptorImpl(receiver.containingDeclaration, newReceiverValue, receiver.annotations)
                )
            }
        }
    }

    private fun NewTypeSubstitutor.toOldSubstitution(): TypeSubstitution = object : TypeSubstitution() {
        override fun get(key: KotlinType): TypeProjection? {
            return safeSubstitute(key.unwrap()).takeIf { it !== key }?.asTypeProjection()
        }

        override fun isEmpty(): Boolean {
            return isEmpty
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

        val typeParametersSubstitutor =
            NewTypeSubstitutorByConstructorMap(
                (callableCandidate.candidate.typeParameters.map { it.typeConstructor } zip resultTypeParameters).toMap()
            )

        val firstSubstitution = typeParametersSubstitutor.toOldSubstitution()
        val secondSubstitution = resultSubstitutor.toOldSubstitution()
        val resultSubstitutor = TypeSubstitutor.createChainedSubstitutor(
            firstSubstitution,
            secondSubstitution
        )

        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceKotlinCallArgumentImpl
        val callableReferenceExpression = psiCallArgument.ktCallableReferenceExpression

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

        val explicitReceiver = explicitCallableReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
        val psiCall = CallMaker.makeCall(reference, explicitReceiver, null, reference, emptyList())

        val tracing = TracingStrategyImpl.create(reference, psiCall)
        val temporaryTrace = TemporaryBindingTrace.create(topLevelTrace, "callable reference fake call")

        val dispatchReceiver = callableCandidate.dispatchReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
        val extensionReceiver = callableCandidate.extensionReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        val resolvedCall = ResolvedCallImpl(
            psiCall, callableCandidate.candidate, dispatchReceiver,
            extensionReceiver, callableCandidate.explicitReceiverKind,
            null, temporaryTrace, tracing, MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
        )
        resolvedCall.setResultingSubstitutor(resultSubstitutor)

        recordArgumentAdaptationForCallableReference(resolvedCall, callableCandidate.callableReferenceAdaptation)

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
        val typeInfo = createTypeInfo(resultType, resolvedAtom.atom.psiCallArgument.dataFlowInfoAfterThisArgument)
        topLevelTrace.record(BindingContext.EXPRESSION_TYPE_INFO, callableReferenceExpression, typeInfo)
        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)

        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(
            callableCandidate.candidate,
            topLevelCallContext.trace,
            callableReferenceExpression
        )

        kotlinToResolvedCallTransformer.runCallCheckers(resolvedCall, topLevelCallCheckerContext)
    }

    private fun ReceiverValue.updateReceiverValue(substitutor: TypeSubstitutor): ReceiverValue {
        val newType = substitutor.safeSubstitute(type, Variance.INVARIANT).let {
            typeApproximator.approximateToSuperType(it, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference) ?: it
        }
        return if (type != newType) replaceType(newType as KotlinType) else this
    }

    private fun recordArgumentAdaptationForCallableReference(
        resolvedCall: ResolvedCallImpl<CallableDescriptor>,
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
            for ((valueParameter, resolvedValueArgument) in mappedArguments) {
                resolvedCall.recordValueArgument(valueParameter, resolvedValueArgument)
            }
        }
    }

    private fun isCallableReferenceWithImplicitConversion(
        resolvedCall: ResolvedCall<CallableDescriptor>,
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
