/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT
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
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils.canBeSmartCast
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils.getSmartCastVariantsExcludingReceiver
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils.isSubTypeBySmartCastIgnoringNullability
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils.recordSmartCastIfNecessary
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionTask
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import java.util.ArrayList

public class CandidateResolver(
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val genericCandidateResolver: GenericCandidateResolver,
        private val reflectionTypes: ReflectionTypes,
        private val modifiersChecker: ModifiersChecker,
        private val additionalTypeCheckers: Iterable<AdditionalTypeChecker>
){

    public fun <D : CallableDescriptor, F : D> performResolutionForCandidateCall(
            context: CallCandidateResolutionContext<D>,
            task: ResolutionTask<D, F>
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

        checkVisibility()

        when (task.checkArguments) {
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
            && !candidateDescriptor.getTypeParameters().isEmpty()
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
        if (!jetTypeArguments.isEmpty()) {
            // Explicit type arguments passed

            val typeArguments = ArrayList<JetType>()
            for (projection in jetTypeArguments) {
                if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                    trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                    ModifierCheckerCore.check(projection, trace, null)
                }
                val type = argumentTypeResolver.resolveTypeRefWithDefault(
                        projection.getTypeReference(), scope, trace,
                        ErrorUtils.createErrorType("Star projection in a call"))!!
                ForceResolveUtil.forceResolveAllContents(type)
                typeArguments.add(type)
            }
            val expectedTypeArgumentCount = candidateDescriptor.getTypeParameters().size()
            for (index in jetTypeArguments.size()..expectedTypeArgumentCount - 1) {
                typeArguments.add(ErrorUtils.createErrorType(
                        "Explicit type argument expected for " + candidateDescriptor.getTypeParameters().get(index).getName()))
            }
            val substitution = FunctionDescriptorUtil.createSubstitution(candidateDescriptor as FunctionDescriptor, typeArguments)
            val substitutor = TypeSubstitutor.create(substitution)

            if (expectedTypeArgumentCount != jetTypeArguments.size()) {
                candidateCall.addStatus(OTHER_ERROR)
                tracing.wrongNumberOfTypeArguments(trace, expectedTypeArgumentCount)
            }
            else {
                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidateDescriptor, substitutor, trace)
            }

            candidateCall.setResultingSubstitutor(substitutor)
        }
        else if (candidateCall.getKnownTypeParametersSubstitutor() != null) {
            candidateCall.setResultingSubstitutor(candidateCall.getKnownTypeParametersSubstitutor()!!)
        }
    }

    private fun <D : CallableDescriptor, F : D> CallCandidateResolutionContext<D>.mapArguments()
            = check {
                val argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                        call, tracing, candidateCall, Sets.newLinkedHashSet<ValueArgument>())
                if (!argumentMappingStatus.isSuccess()) {
                    candidateCall.addStatus(OTHER_ERROR)
                }
            }

    private fun <D : CallableDescriptor, F : D> CallCandidateResolutionContext<D>.checkExpectedCallableType()
            = check {
                if (!noExpectedType(expectedType)) {
                    val candidate = candidateCall.getCandidateDescriptor()
                    val candidateReflectionType = getReflectionTypeForCandidateDescriptor(candidate, this, reflectionTypes);
                    if (candidateReflectionType != null) {
                        if (!JetTypeChecker.DEFAULT.isSubtypeOf(candidateReflectionType, expectedType)) {
                            candidateCall.addStatus(OTHER_ERROR)
                        }
                    }
                }
            }

    private fun CallCandidateResolutionContext<*>.checkVisibility() = checkAndReport {
        val receiverValue = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidateCall.getDispatchReceiver(),
                                                                                      trace.getBindingContext())
        val invisibleMember = Visibilities.findInvisibleMember(receiverValue, candidateDescriptor, scope.getContainingDeclaration())
        if (invisibleMember != null) {
            tracing.invisibleMember(trace, invisibleMember)
            OTHER_ERROR
        } else {
            SUCCESS
        }
    }

    private fun CallCandidateResolutionContext<*>.checkExtensionReceiver() = checkAndReport {
        val receiverParameter = candidateCall.getCandidateDescriptor().getExtensionReceiverParameter()
        val receiverArgument = candidateCall.getExtensionReceiver()
        if (receiverParameter != null && !receiverArgument.exists()) {
            tracing.missingReceiver(candidateCall.getTrace(), receiverParameter)
            OTHER_ERROR
        }
        else if (receiverParameter == null && receiverArgument.exists()) {
            tracing.noReceiverAllowed(candidateCall.getTrace())
            if (call.getCalleeExpression() is JetSimpleNameExpression) {
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
        if (dispatchReceiver.exists()) {
            var nestedClass: ClassDescriptor? = null
            if (candidateDescriptor is ConstructorDescriptor
                && DescriptorUtils.isStaticNestedClass(candidateDescriptor.getContainingDeclaration())
            ) {
                nestedClass = candidateDescriptor.getContainingDeclaration()
            }
            else if (candidateDescriptor is FakeCallableDescriptorForObject) {
                nestedClass = candidateDescriptor.getReferencedDescriptor()
            }
            if (nestedClass != null) {
                tracing.nestedClassAccessViaInstanceReference(trace, nestedClass, candidateCall.getExplicitReceiverKind())
                return@checkAndReport OTHER_ERROR
            }
        }

        assert((dispatchReceiver.exists() == (candidateCall.getResultingDescriptor().getDispatchReceiverParameter() != null))) {
            "Shouldn't happen because of TaskPrioritizer: $candidateDescriptor"
        }

        SUCCESS
    }

    private fun checkOuterClassMemberIsAccessible(context: CallCandidateResolutionContext<*>): Boolean {
        // In "this@Outer.foo()" the error will be reported on "this@Outer" instead
        if (context.call.getExplicitReceiver().exists() || context.call.getDispatchReceiver().exists()) return true

        val candidateThis = getDeclaringClass(context.candidateCall.getCandidateDescriptor())
        if (candidateThis == null || candidateThis.getKind().isSingleton()) return true

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.getCallElement(), candidateThis)
    }

    private fun CallCandidateResolutionContext<*>.checkAbstractAndSuper() = check {
        val descriptor = candidateDescriptor
        val expression = candidateCall.getCall().getCalleeExpression()

        if (expression is JetSimpleNameExpression) {
            // 'B' in 'class A: B()' is JetConstructorCalleeExpression
            if (descriptor is ConstructorDescriptor) {
                val modality = descriptor.getContainingDeclaration().getModality()
                if (modality == Modality.ABSTRACT) {
                    tracing.instantiationOfAbstractClass(trace)
                }
            }
        }

        val superDispatchReceiver = getReceiverSuper(candidateCall.getDispatchReceiver())
        if (superDispatchReceiver != null) {
            if (descriptor is MemberDescriptor && descriptor.getModality() == Modality.ABSTRACT) {
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
        if (isSynthesizedInvoke(candidateCall.getCandidateDescriptor())
            && !KotlinBuiltIns.isExtensionFunctionType(candidateCall.getDispatchReceiver().getType())
        ) {
            tracing.freeFunctionCalledAsExtension(trace)
            OTHER_ERROR
        } else {
            SUCCESS
        }
    }

    private fun getReceiverSuper(receiver: ReceiverValue): JetSuperExpression? {
        if (receiver is ExpressionReceiver) {
            val expression = receiver.getExpression()
            if (expression is JetSuperExpression) {
                return expression
            }
        }
        return null
    }

    private fun getDeclaringClass(candidate: CallableDescriptor): ClassDescriptor? {
        val expectedThis = candidate.getDispatchReceiverParameter() ?: return null
        val descriptor = expectedThis.getContainingDeclaration()
        return if (descriptor is ClassDescriptor) descriptor else null
    }

    public fun <D : CallableDescriptor> checkAllValueArguments(
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
        val argumentTypes = Lists.newArrayList<JetType>()
        val infoForArguments = candidateCall.getDataFlowInfoForArguments()
        for (entry in candidateCall.getValueArguments().entrySet()) {
            val parameterDescriptor = entry.getKey()
            val resolvedArgument = entry.getValue()


            for (argument in resolvedArgument.getArguments()) {
                val expression = argument.getArgumentExpression() ?: continue

                val expectedType = getEffectiveExpectedType(parameterDescriptor, argument)

                val newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument)).replaceExpectedType(expectedType)
                val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(
                        expression, newContext, resolveFunctionArgumentBodies)
                val type = typeInfoForCall.type
                infoForArguments.updateInfo(argument, typeInfoForCall.dataFlowInfo)

                var matchStatus = ArgumentMatchStatus.SUCCESS
                var resultingType: JetType? = type
                if (type == null || (type.isError() && !ErrorUtils.isFunctionPlaceholder(type))) {
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
                }
                argumentTypes.add(resultingType)
                candidateCall.recordArgumentMatchStatus(argument, matchStatus)
            }
        }
        return ValueArgumentsCheckingResult(resultStatus, argumentTypes)
    }

    private fun smartCastValueArgumentTypeIfPossible(
            expression: JetExpression,
            expectedType: JetType,
            actualType: JetType,
            context: ResolutionContext<*>): JetType? {
        val receiverToCast = ExpressionReceiver(JetPsiUtil.safeDeparenthesize(expression, false), actualType)
        val variants = getSmartCastVariantsExcludingReceiver(context, receiverToCast)
        for (possibleType in variants) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(possibleType, expectedType)) {
                return possibleType
            }
        }
        return null
    }

    private fun CallCandidateResolutionContext<*>.checkReceiverTypeError(): Unit = check {
        val extensionReceiver = candidateDescriptor.getExtensionReceiverParameter()
        val dispatchReceiver = candidateDescriptor.getDispatchReceiverParameter()

        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!isInvokeCallOnExpressionWithBothReceivers(call)) {
            checkReceiverTypeError(extensionReceiver, candidateCall.getExtensionReceiver())
        }
        checkReceiverTypeError(dispatchReceiver, candidateCall.getDispatchReceiver())
    }

    private fun CallCandidateResolutionContext<*>.checkReceiverTypeError(
            receiverParameterDescriptor: ReceiverParameterDescriptor?,
            receiverArgument: ReceiverValue
    ) = checkAndReport {
        if (receiverParameterDescriptor == null || !receiverArgument.exists()) return@checkAndReport SUCCESS

        val erasedReceiverType = getErasedReceiverType(receiverParameterDescriptor, candidateDescriptor)

        if (!isSubTypeBySmartCastIgnoringNullability(receiverArgument, erasedReceiverType, this)) {
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
                candidateCall.getResultingDescriptor().getExtensionReceiverParameter(),
                candidateCall.getExtensionReceiver(), candidateCall.getExplicitReceiverKind().isExtensionReceiver(), false))

        resultStatus = resultStatus.combine(context.checkReceiver(candidateCall,
                                                                  candidateCall.getResultingDescriptor().getDispatchReceiverParameter(), candidateCall.getDispatchReceiver(),
                                                                  candidateCall.getExplicitReceiverKind().isDispatchReceiver(),
                // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                                                                  context.call is CallForImplicitInvoke))
        return resultStatus
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.checkReceiver(
            candidateCall: ResolvedCall<D>,
            receiverParameter: ReceiverParameterDescriptor?,
            receiverArgument: ReceiverValue,
            isExplicitReceiver: Boolean,
            implicitInvokeCheck: Boolean): ResolutionStatus {
        if (receiverParameter == null || !receiverArgument.exists()) return SUCCESS
        val candidateDescriptor = candidateCall.getCandidateDescriptor()
        if (TypeUtils.dependsOnTypeParameters(receiverParameter.getType(), candidateDescriptor.getTypeParameters())) return SUCCESS

        val safeAccess = isExplicitReceiver && !implicitInvokeCheck && candidateCall.getCall().isExplicitSafeCall()
        val isSubtypeBySmartCast = isSubTypeBySmartCastIgnoringNullability(
                receiverArgument, receiverParameter.getType(), this)
        if (!isSubtypeBySmartCast) {
            tracing.wrongReceiverType(trace, receiverParameter, receiverArgument)
            return OTHER_ERROR
        }
        if (!recordSmartCastIfNecessary(receiverArgument, receiverParameter.getType(), this, safeAccess)) {
            return OTHER_ERROR
        }

        val receiverArgumentType = receiverArgument.getType()

        val bindingContext = trace.getBindingContext()
        if (!safeAccess && !receiverParameter.getType().isMarkedNullable() && receiverArgumentType.isMarkedNullable()) {
            if (!canBeSmartCast(receiverParameter, receiverArgument, this)) {
                tracing.unsafeCall(trace, receiverArgumentType, implicitInvokeCheck)
                return UNSAFE_CALL_ERROR
            }
        }
        val receiverValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, bindingContext, scope.getContainingDeclaration())
        if (safeAccess && !dataFlowInfo.getNullability(receiverValue).canBeNull()) {
            tracing.unnecessarySafeCall(trace, receiverArgumentType)
        }

        additionalTypeCheckers.forEach { it.checkReceiver(receiverParameter, receiverArgument, safeAccess, this) }

        return SUCCESS
    }

    public inner class ValueArgumentsCheckingResult(public val status: ResolutionStatus, public val argumentTypes: List<JetType>)

    private fun checkGenericBoundsInAFunctionCall(
            jetTypeArguments: List<JetTypeProjection>,
            typeArguments: List<JetType>,
            functionDescriptor: CallableDescriptor,
            substitutor: TypeSubstitutor,
            trace: BindingTrace) {
        val typeParameters = functionDescriptor.getTypeParameters()
        for (i in 0..Math.min(typeParameters.size(), jetTypeArguments.size()) - 1) {
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

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>
            .check(checker: CallCandidateResolutionContext<D>.() -> Unit): Unit = if (shouldContinue()) checker()

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.
            checkAndReport(checker: CallCandidateResolutionContext<D>.() -> ResolutionStatus) {
        if (shouldContinue()) {
            candidateCall.addStatus(checker())
        }
    }

    private val CallCandidateResolutionContext<*>.candidateDescriptor: CallableDescriptor get() = candidateCall.getCandidateDescriptor()

}
