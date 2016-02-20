/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.SUPER_CANT_BE_EXTENSION_RECEIVER
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.callableReferences.getReflectionTypeForCandidateDescriptor
import org.jetbrains.kotlin.resolve.calls.CallTransformer.CallForImplicitInvoke
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getErasedReceiverType
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isInvokeCallOnExpressionWithBothReceivers
import org.jetbrains.kotlin.resolve.calls.callUtil.isExplicitSafeCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.inference.SubstitutionFilteringInternalResolveAnnotations
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class CandidateResolver(
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val genericCandidateResolver: GenericCandidateResolver,
        private val reflectionTypes: ReflectionTypes,
        private val smartCastManager: SmartCastManager
) {

    fun <D : CallableDescriptor, F : D> performResolutionForCandidateCall(
            context: CallCandidateResolutionContext<D>,
            checkArguments: CheckArgumentTypesMode
    ): Unit = with(context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (ErrorUtils.isError(candidateDescriptor)) {
            candidateCall.addStatus(SUCCESS)
            return
        }

        if (!checkOuterClassMemberIsAccessible(this)) {
            candidateCall.addStatus(OTHER_ERROR)
            return
        }

        if (!context.isDebuggerContext) {
            checkVisibility()
        }

        when (checkArguments) {
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ->
                mapArguments()
            CheckArgumentTypesMode.CHECK_CALLABLE_TYPE ->
                checkExpectedCallableType()
        }

        checkReceiverTypeError()
        checkExtensionReceiver()
        checkDispatchReceiver()

        processTypeArguments()
        checkValueArguments()

        checkAbstractAndSuper()
        checkNonExtensionCalledWithReceiver()
    }

    private fun CallCandidateResolutionContext<*>.checkValueArguments() = checkAndReport {
        if (call.getTypeArguments().isEmpty()
            && !candidateDescriptor.typeParameters.isEmpty()
            && candidateCall.getKnownTypeParametersSubstitutor() == null
        ) {
            genericCandidateResolver.inferTypeArguments(this)
        }
        else {
            checkAllValueArguments(this, SHAPE_FUNCTION_ARGUMENTS).status
        }
    }

    private fun CallCandidateResolutionContext<*>.processTypeArguments() = check {
        val jetTypeArguments = call.getTypeArguments()
        if (candidateCall.knownTypeParametersSubstitutor != null) {
            candidateCall.setResultingSubstitutor(candidateCall.knownTypeParametersSubstitutor!!)
        }
        else if (!jetTypeArguments.isEmpty()) {
            // Explicit type arguments passed

            val typeArguments = ArrayList<KotlinType>()
            for (projection in jetTypeArguments) {
                val type = projection.typeReference?.let { trace.bindingContext.get(BindingContext.TYPE, it) }
                        ?: ErrorUtils.createErrorType("Star projection in a call")
                typeArguments.add(type)
            }

            val expectedTypeArgumentCount = candidateDescriptor.typeParameters.size
            for (index in jetTypeArguments.size..expectedTypeArgumentCount - 1) {
                typeArguments.add(ErrorUtils.createErrorType(
                        "Explicit type argument expected for " + candidateDescriptor.typeParameters.get(index).name))
            }
            val substitution = FunctionDescriptorUtil.createSubstitution(candidateDescriptor as FunctionDescriptor, typeArguments)
            val substitutor = TypeSubstitutor.create(SubstitutionFilteringInternalResolveAnnotations(substitution))

            if (expectedTypeArgumentCount != jetTypeArguments.size) {
                candidateCall.addStatus(OTHER_ERROR)
                tracing.wrongNumberOfTypeArguments(trace, expectedTypeArgumentCount)
            }
            else {
                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidateDescriptor, substitutor, trace)
            }

            candidateCall.setResultingSubstitutor(substitutor)
        }
    }

    private fun <D : CallableDescriptor, F : D> CallCandidateResolutionContext<D>.mapArguments()
            = check {
                val argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                        call, tracing, candidateCall, Sets.newLinkedHashSet<ValueArgument>())
                if (!argumentMappingStatus.isSuccess) {
                    candidateCall.addStatus(ARGUMENTS_MAPPING_ERROR)
                }
            }

    private fun <D : CallableDescriptor, F : D> CallCandidateResolutionContext<D>.checkExpectedCallableType()
            = check {
                if (!noExpectedType(expectedType)) {
                    val candidate = candidateCall.getCandidateDescriptor()
                    val candidateReflectionType = getReflectionTypeForCandidateDescriptor(candidate, reflectionTypes);
                    if (candidateReflectionType != null) {
                        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(candidateReflectionType, expectedType)) {
                            candidateCall.addStatus(OTHER_ERROR)
                        }
                    }
                    else {
                        candidateCall.addStatus(OTHER_ERROR)
                    }
                }
            }

    private fun CallCandidateResolutionContext<*>.checkVisibility() = checkAndReport {
        val invisibleMember = Visibilities.findInvisibleMember(candidateCall.dispatchReceiver, candidateDescriptor, scope.ownerDescriptor)
        if (invisibleMember != null) {
            tracing.invisibleMember(trace, invisibleMember)
            OTHER_ERROR
        } else {
            SUCCESS
        }
    }

    private fun CallCandidateResolutionContext<*>.checkExtensionReceiver() = checkAndReport {
        val receiverParameter = candidateCall.getCandidateDescriptor().extensionReceiverParameter
        val receiverArgument = candidateCall.getExtensionReceiver()
        if (receiverParameter != null && receiverArgument == null) {
            tracing.missingReceiver(candidateCall.getTrace(), receiverParameter)
            OTHER_ERROR
        }
        else if (receiverParameter == null && receiverArgument != null) {
            tracing.noReceiverAllowed(candidateCall.getTrace())
            if (call.getCalleeExpression() is KtSimpleNameExpression) {
                RECEIVER_PRESENCE_ERROR
            }
            else {
                OTHER_ERROR
            }
        }
        else {
            SUCCESS
        }
    }

    private fun CallCandidateResolutionContext<*>.checkDispatchReceiver() = checkAndReport {
        val candidateDescriptor = candidateDescriptor
        val dispatchReceiver = candidateCall.getDispatchReceiver()
        if (dispatchReceiver != null) {
            var nestedClass: ClassDescriptor? = null
            if (candidateDescriptor is ConstructorDescriptor
                && DescriptorUtils.isStaticNestedClass(candidateDescriptor.containingDeclaration)
            ) {
                nestedClass = candidateDescriptor.containingDeclaration
            }
            else if (candidateDescriptor is FakeCallableDescriptorForObject) {
                nestedClass = candidateDescriptor.getReferencedDescriptor()
            }
            if (nestedClass != null) {
                tracing.nestedClassAccessViaInstanceReference(trace, nestedClass, candidateCall.getExplicitReceiverKind())
                return@checkAndReport OTHER_ERROR
            }
        }

        assert((dispatchReceiver != null) == (candidateCall.getResultingDescriptor().dispatchReceiverParameter != null)) {
            "Shouldn't happen because of TaskPrioritizer: $candidateDescriptor"
        }

        SUCCESS
    }

    private fun checkOuterClassMemberIsAccessible(context: CallCandidateResolutionContext<*>): Boolean {

        fun KtElement.insideScript() = (containingFile as? KtFile)?.isScript ?: false

        // context.scope doesn't contains outer class implicit receiver if we inside nested class
        // Outer scope for some class in script file is scopeForInitializerResolution see: DeclarationScopeProviderImpl.getResolutionScopeForDeclaration
        if (!context.call.callElement.insideScript()) return true

        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver() != null || context.call.getDispatchReceiver() != null) return true

        val candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor())
        if (candidateThis == null || candidateThis.kind.isSingleton) return true

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.getCallElement(), candidateThis)
    }

    private fun CallCandidateResolutionContext<*>.checkAbstractAndSuper() = check {
        val descriptor = candidateDescriptor
        val expression = candidateCall.getCall().getCalleeExpression()

        if (expression is KtSimpleNameExpression) {
            // 'B' in 'class A: B()' is JetConstructorCalleeExpression
            if (descriptor is ConstructorDescriptor) {
                val modality = descriptor.containingDeclaration.modality
                if (modality == Modality.ABSTRACT) {
                    tracing.instantiationOfAbstractClass(trace)
                }
            }
        }

        val superDispatchReceiver = getReceiverSuper(candidateCall.getDispatchReceiver())
        if (superDispatchReceiver != null) {
            if (descriptor is MemberDescriptor && descriptor.modality == Modality.ABSTRACT) {
                tracing.abstractSuperCall(trace)
                candidateCall.addStatus(OTHER_ERROR)
            }
        }

        // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
        // See TaskPrioritizer for more
        val superExtensionReceiver = getReceiverSuper(candidateCall.getExtensionReceiver())
        if (superExtensionReceiver != null) {
            trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(superExtensionReceiver, superExtensionReceiver.getText()))
            candidateCall.addStatus(OTHER_ERROR)
        }
    }

    private fun CallCandidateResolutionContext<*>.checkNonExtensionCalledWithReceiver() = checkAndReport {
        val call = candidateCall.call
        if (call is CallTransformer.CallForImplicitInvoke && candidateCall.extensionReceiver != null
                && candidateCall.dispatchReceiver != null
        ) {
            if (call.dispatchReceiver == candidateCall.dispatchReceiver
                    && !KotlinBuiltIns.isExactExtensionFunctionType(call.dispatchReceiver.type)
            ) {
                tracing.nonExtensionFunctionCalledAsExtension(trace)
                return@checkAndReport OTHER_ERROR
            }
        }
        SUCCESS
    }

    private fun getReceiverSuper(receiver: Receiver?): KtSuperExpression? {
        if (receiver is ExpressionReceiver) {
            val expression = receiver.expression
            if (expression is KtSuperExpression) {
                return expression
            }
        }
        return null
    }

    private fun getDeclaringClass(candidate: CallableDescriptor): ClassDescriptor? {
        val expectedThis = candidate.dispatchReceiverParameter ?: return null
        val descriptor = expectedThis.containingDeclaration
        return if (descriptor is ClassDescriptor) descriptor else null
    }

    fun <D : CallableDescriptor> checkAllValueArguments(
            context: CallCandidateResolutionContext<D>,
            resolveFunctionArgumentBodies: ResolveArgumentsMode): ValueArgumentsCheckingResult {
        val checkingResult = checkValueArgumentTypes(context, context.candidateCall, resolveFunctionArgumentBodies)
        var resultStatus = checkingResult.status
        resultStatus = resultStatus.combine(checkReceivers(context))

        return ValueArgumentsCheckingResult(resultStatus, checkingResult.argumentTypes)
    }

    private fun <D : CallableDescriptor, C : CallResolutionContext<C>> checkValueArgumentTypes(
            context: CallResolutionContext<C>,
            candidateCall: MutableResolvedCall<D>,
            resolveFunctionArgumentBodies: ResolveArgumentsMode): ValueArgumentsCheckingResult {
        var resultStatus = SUCCESS
        val argumentTypes = Lists.newArrayList<KotlinType>()
        val infoForArguments = candidateCall.getDataFlowInfoForArguments()
        for (entry in candidateCall.getValueArguments().entries) {
            val parameterDescriptor = entry.key
            val resolvedArgument = entry.value


            for (argument in resolvedArgument.arguments) {
                val expression = argument.getArgumentExpression() ?: continue

                val expectedType = getEffectiveExpectedType(parameterDescriptor, argument)

                val newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument)).replaceExpectedType(expectedType)
                val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                        expression, newContext, resolveFunctionArgumentBodies)
                val type = typeInfoForCall.type
                infoForArguments.updateInfo(argument, typeInfoForCall.dataFlowInfo)

                var matchStatus = ArgumentMatchStatus.SUCCESS
                var resultingType: KotlinType? = type
                if (type == null || (type.isError && !type.isFunctionPlaceholder)) {
                    matchStatus = ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE
                }
                else if (!noExpectedType(expectedType)) {
                    if (!ArgumentTypeResolver.isSubtypeOfForArgumentType(type, expectedType)) {
                        val smartCast = smartCastValueArgumentTypeIfPossible(expression, newContext.expectedType, type, newContext)
                        if (smartCast == null) {
                            resultStatus = OTHER_ERROR
                            matchStatus = ArgumentMatchStatus.TYPE_MISMATCH
                        }
                        else {
                            resultingType = smartCast
                        }
                    }
                    else if (ErrorUtils.containsUninferredParameter(expectedType)) {
                        matchStatus = ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES
                    }

                    val spreadElement = argument.getSpreadElement()
                    if (spreadElement != null && !type.isFlexible() && type.isMarkedNullable) {
                        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, context)
                        val smartCastResult = SmartCastManager.checkAndRecordPossibleCast(dataFlowValue, expectedType, expression, context, null, false)
                        if (smartCastResult == null || !smartCastResult.isCorrect) {
                            context.trace.report(Errors.SPREAD_OF_NULLABLE.on(spreadElement));
                        }
                    }
                }
                argumentTypes.add(resultingType)
                candidateCall.recordArgumentMatchStatus(argument, matchStatus)
            }
        }
        return ValueArgumentsCheckingResult(resultStatus, argumentTypes)
    }

    private fun smartCastValueArgumentTypeIfPossible(
            expression: KtExpression,
            expectedType: KotlinType,
            actualType: KotlinType,
            context: ResolutionContext<*>): KotlinType? {
        val receiverToCast = ExpressionReceiver.create(KtPsiUtil.safeDeparenthesize(expression), actualType, context.trace.bindingContext)
        val variants = smartCastManager.getSmartCastVariantsExcludingReceiver(context, receiverToCast)
        for (possibleType in variants) {
            if (KotlinTypeChecker.DEFAULT.isSubtypeOf(possibleType, expectedType)) {
                return possibleType
            }
        }
        return null
    }

    private fun CallCandidateResolutionContext<*>.checkReceiverTypeError(): Unit = check {
        val extensionReceiver = candidateDescriptor.extensionReceiverParameter
        val dispatchReceiver = candidateDescriptor.dispatchReceiverParameter

        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!isInvokeCallOnExpressionWithBothReceivers(call)) {
            val callExtensionReceiver = candidateCall.extensionReceiver
            assert(callExtensionReceiver is ReceiverValue?) { "Expected ReceiverValue, got $callExtensionReceiver" }
            checkReceiverTypeError(extensionReceiver, callExtensionReceiver as ReceiverValue?)
        }
        checkReceiverTypeError(dispatchReceiver, candidateCall.getDispatchReceiver())
    }

    private fun CallCandidateResolutionContext<*>.checkReceiverTypeError(
            receiverParameterDescriptor: ReceiverParameterDescriptor?,
            receiverArgument: ReceiverValue?
    ) = checkAndReport {
        if (receiverParameterDescriptor == null || receiverArgument == null) return@checkAndReport SUCCESS

        val erasedReceiverType = getErasedReceiverType(receiverParameterDescriptor, candidateDescriptor)

        if (!smartCastManager.isSubTypeBySmartCastIgnoringNullability(receiverArgument, erasedReceiverType, this)) {
            RECEIVER_TYPE_ERROR
        } else {
            SUCCESS
        }
    }

    private fun <D : CallableDescriptor> checkReceivers(context: CallCandidateResolutionContext<D>): ResolutionStatus {
        var resultStatus = SUCCESS
        val candidateCall = context.candidateCall

        // Comment about a very special case.
        // Call 'b.foo(1)' where class 'Foo' has an extension member 'fun B.invoke(Int)' should be checked two times for safe call (in 'checkReceiver'), because
        // both 'b' (receiver) and 'foo' (this object) might be nullable. In the first case we mark dot, in the second 'foo'.
        // Class 'CallForImplicitInvoke' helps up to recognise this case, and parameter 'implicitInvokeCheck' helps us to distinguish whether we check receiver or this object.

        resultStatus = resultStatus.combine(context.checkReceiver(
                candidateCall,
                candidateCall.getResultingDescriptor().extensionReceiverParameter,
                candidateCall.extensionReceiver as ReceiverValue?,
                candidateCall.getExplicitReceiverKind().isExtensionReceiver, false))

        resultStatus = resultStatus.combine(context.checkReceiver(candidateCall,
                                                                  candidateCall.getResultingDescriptor().dispatchReceiverParameter, candidateCall.getDispatchReceiver(),
                                                                  candidateCall.getExplicitReceiverKind().isDispatchReceiver,
                // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                                                                  context.call is CallForImplicitInvoke))
        return resultStatus
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.checkReceiver(
            candidateCall: MutableResolvedCall<D>,
            receiverParameter: ReceiverParameterDescriptor?,
            receiverArgument: ReceiverValue?,
            isExplicitReceiver: Boolean,
            implicitInvokeCheck: Boolean): ResolutionStatus {
        if (receiverParameter == null || receiverArgument == null) return SUCCESS
        val candidateDescriptor = candidateCall.candidateDescriptor
        if (TypeUtils.dependsOnTypeParameters(receiverParameter.type, candidateDescriptor.typeParameters)) return SUCCESS

        val isSubtypeBySmartCastIgnoringNullability = smartCastManager.isSubTypeBySmartCastIgnoringNullability(
                receiverArgument, receiverParameter.type, this)

        if (!isSubtypeBySmartCastIgnoringNullability) {
            tracing.wrongReceiverType(
                    trace, receiverParameter, receiverArgument,
                    this.replaceCallPosition(CallPosition.ExtensionReceiverPosition(candidateCall)))
            return OTHER_ERROR
        }

        // Here we know that receiver is OK ignoring nullability and check that nullability is OK too
        // Doing it simply as full subtyping check (receiverValueType <: receiverParameterType)
        val call = candidateCall.call
        val safeAccess = isExplicitReceiver && !implicitInvokeCheck && call.isExplicitSafeCall()
        val expectedReceiverParameterType = if (safeAccess) TypeUtils.makeNullable(receiverParameter.type) else receiverParameter.type
        val smartCastNeeded = !ArgumentTypeResolver.isSubtypeOfForArgumentType(receiverArgument.type, expectedReceiverParameterType)
        var reportUnsafeCall = false

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, this)
        val nullability = dataFlowInfo.getPredictableNullability(dataFlowValue)
        var nullableImplicitInvokeReceiver = false
        var receiverArgumentType = receiverArgument.type
        if (implicitInvokeCheck && call is CallForImplicitInvoke && call.isSafeCall()) {
            val outerCallReceiver = call.outerCall.explicitReceiver
            if (outerCallReceiver != call.explicitReceiver && outerCallReceiver is ReceiverValue) {
                val outerReceiverDataFlowValue = DataFlowValueFactory.createDataFlowValue(outerCallReceiver, this)
                val outerReceiverNullability = dataFlowInfo.getPredictableNullability(outerReceiverDataFlowValue)
                if (outerReceiverNullability.canBeNull() && !TypeUtils.isNullableType(expectedReceiverParameterType)) {
                    nullableImplicitInvokeReceiver = true
                    receiverArgumentType = TypeUtils.makeNullable(receiverArgumentType)
                }
            }
        }
        val expression = (receiverArgument as? ExpressionReceiver)?.expression
        if (nullability.canBeNull() && !nullability.canBeNonNull()) {
            if (!TypeUtils.isNullableType(expectedReceiverParameterType)) {
                reportUnsafeCall = true
            }
            if (dataFlowValue.immanentNullability.canBeNonNull()) {
                expression?.let { trace.record(BindingContext.SMARTCAST_NULL, it) }
            }
        }
        else if (!nullableImplicitInvokeReceiver && smartCastNeeded) {
            // Look if smart cast has some useful nullability info

            val smartCastResult = SmartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, expectedReceiverParameterType, expression, this, candidateCall.call.calleeExpression, /*recordType =*/true
            )

            if (smartCastResult == null) {
                reportUnsafeCall = true
            }
            else {
                candidateCall.setSmartCastDispatchReceiverType(smartCastResult.resultType)
                if (!smartCastResult.isCorrect) {
                    // Error about unstable smart cast reported within checkAndRecordPossibleCast
                    return OTHER_ERROR
                }
            }
        }

        if (reportUnsafeCall || nullableImplicitInvokeReceiver) {
            tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck)
            return UNSAFE_CALL_ERROR
        }

        return SUCCESS
    }

    inner class ValueArgumentsCheckingResult(val status: ResolutionStatus, val argumentTypes: List<KotlinType>)

    private fun checkGenericBoundsInAFunctionCall(
            jetTypeArguments: List<KtTypeProjection>,
            typeArguments: List<KotlinType>,
            functionDescriptor: CallableDescriptor,
            substitutor: TypeSubstitutor,
            trace: BindingTrace) {
        val typeParameters = functionDescriptor.typeParameters
        for (i in 0..Math.min(typeParameters.size, jetTypeArguments.size) - 1) {
            val typeParameterDescriptor = typeParameters.get(i)
            val typeArgument = typeArguments.get(i)
            val typeReference = jetTypeArguments.get(i).getTypeReference()
            if (typeReference != null) {
                DescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace)
            }
        }
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.shouldContinue() =
            candidateResolveMode == CandidateResolveMode.FULLY || candidateCall.getStatus().possibleTransformToSuccess()

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.check(
            checker: CallCandidateResolutionContext<D>.() -> Unit
    ) {
        if (shouldContinue()) checker()
    }

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.
            checkAndReport(checker: CallCandidateResolutionContext<D>.() -> ResolutionStatus) {
        if (shouldContinue()) {
            candidateCall.addStatus(checker())
        }
    }

    private val CallCandidateResolutionContext<*>.candidateDescriptor: CallableDescriptor get() = candidateCall.getCandidateDescriptor()

}
