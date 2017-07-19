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

package org.jetbrains.kotlin.resolve.calls

import com.google.common.collect.Lists
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
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
import org.jetbrains.kotlin.resolve.calls.smartcasts.getReceiverValueWithSmartCast
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.ErrorTypesAreEqualToAnything
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.typeUtil.containsTypeProjectionsInTopLevelArguments
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.*

class CandidateResolver(
        private val argumentTypeResolver: ArgumentTypeResolver,
        private val genericCandidateResolver: GenericCandidateResolver,
        private val reflectionTypes: ReflectionTypes,
        private val smartCastManager: SmartCastManager
) {
    fun <D : CallableDescriptor> performResolutionForCandidateCall(
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
            checkVisibilityWithoutReceiver()
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
        checkConstructedExpandedType()
    }

    private fun CallCandidateResolutionContext<*>.checkValueArguments() = checkAndReport {
        if (call.typeArguments.isEmpty()
            && !candidateDescriptor.typeParameters.isEmpty()
            && candidateCall.knownTypeParametersSubstitutor == null
        ) {
            genericCandidateResolver.inferTypeArguments(this)
        }
        else {
            checkAllValueArguments(this, SHAPE_FUNCTION_ARGUMENTS).status
        }
    }

    private fun CallCandidateResolutionContext<*>.processTypeArguments() = check {
        val ktTypeArguments = call.typeArguments
        if (candidateCall.knownTypeParametersSubstitutor != null) {
            candidateCall.setResultingSubstitutor(candidateCall.knownTypeParametersSubstitutor!!)
        }
        else if (ktTypeArguments.isNotEmpty()) {
            // Explicit type arguments passed

            val typeArguments = ArrayList<KotlinType>()
            for (projection in ktTypeArguments) {
                val type = projection.typeReference?.let { trace.bindingContext.get(BindingContext.TYPE, it) }
                        ?: ErrorUtils.createErrorType("Star projection in a call")
                typeArguments.add(type)
            }

            val expectedTypeArgumentCount = candidateDescriptor.typeParameters.size
            for (index in ktTypeArguments.size..expectedTypeArgumentCount - 1) {
                typeArguments.add(ErrorUtils.createErrorType(
                        "Explicit type argument expected for " + candidateDescriptor.typeParameters[index].name
                ))
            }
            val substitution = FunctionDescriptorUtil.createSubstitution(candidateDescriptor as FunctionDescriptor, typeArguments)
            val substitutor = TypeSubstitutor.create(SubstitutionFilteringInternalResolveAnnotations(substitution))

            if (expectedTypeArgumentCount != ktTypeArguments.size) {
                candidateCall.addStatus(WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR)
                tracing.wrongNumberOfTypeArguments(trace, expectedTypeArgumentCount, candidateDescriptor)
            }
            else {
                checkGenericBoundsInAFunctionCall(ktTypeArguments, typeArguments, candidateDescriptor, substitutor, trace)
            }

            candidateCall.setResultingSubstitutor(substitutor)
        }
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.mapArguments()
            = check {
                val argumentMappingStatus = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(
                        call, tracing, candidateCall)
                if (!argumentMappingStatus.isSuccess) {
                    candidateCall.addStatus(ARGUMENTS_MAPPING_ERROR)
                }
            }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.checkExpectedCallableType()
            = check {
                if (!noExpectedType(expectedType)) {
                    val candidateKCallableType = DoubleColonExpressionResolver.createKCallableTypeForReference(
                            candidateCall.candidateDescriptor,
                            (call.callElement.parent as? KtCallableReferenceExpression)?.receiverExpression?.let {
                                trace.bindingContext.get(BindingContext.DOUBLE_COLON_LHS, it)
                            },
                            reflectionTypes, scope.ownerDescriptor
                    )
                    if (candidateKCallableType == null ||
                        !canBeSubtype(candidateKCallableType, expectedType, candidateCall.candidateDescriptor.typeParameters)) {
                        candidateCall.addStatus(OTHER_ERROR)
                    }
                }
            }

    private fun canBeSubtype(subType: KotlinType, superType: KotlinType, candidateTypeParameters: List<TypeParameterDescriptor>): Boolean {
        // Here we need to check that there exists a substitution from type parameters (used in types in candidate signature)
        // to arguments such that substituted candidateKCallableType would be a subtype of expectedType.
        // It looks like in general this can only be decided by constructing a constraint system and checking
        // if it has a contradiction. Currently we use a heuristic that may not work ideally in all cases.
        // TODO: use constraint system to check if candidateKCallableType can be a subtype of expectedType
        val substituteDontCare = makeConstantSubstitutor(candidateTypeParameters, TypeUtils.DONT_CARE)
        val subTypeSubstituted = substituteDontCare.substitute(subType, Variance.INVARIANT) ?: return true
        return ErrorTypesAreEqualToAnything.isSubtypeOf(subTypeSubstituted, superType)
    }

    private fun CallCandidateResolutionContext<*>.checkVisibilityWithoutReceiver() = checkAndReport {
        checkVisibilityWithDispatchReceiver(Visibilities.ALWAYS_SUITABLE_RECEIVER, null)
    }

    private fun CallCandidateResolutionContext<*>.checkVisibilityWithDispatchReceiver(
            receiverArgument: ReceiverValue?,
            smartCastType: KotlinType?
    ): ResolutionStatus {
        val invisibleMember = Visibilities.findInvisibleMember(
                getReceiverValueWithSmartCast(receiverArgument, smartCastType), candidateDescriptor, scope.ownerDescriptor)
        return if (invisibleMember != null) {
            tracing.invisibleMember(trace, invisibleMember)
            INVISIBLE_MEMBER_ERROR
        } else {
            SUCCESS
        }
    }

    private fun CallCandidateResolutionContext<*>.isCandidateVisibleOrExtensionReceiver(
            receiverArgument: ReceiverValue?,
            smartCastType: KotlinType?,
            isDispatchReceiver: Boolean
    ) = !isDispatchReceiver || isCandidateVisible(receiverArgument, smartCastType)

    private fun CallCandidateResolutionContext<*>.isCandidateVisible(
            receiverArgument: ReceiverValue?,
            smartCastType: KotlinType?
    ) = Visibilities.findInvisibleMember(
            getReceiverValueWithSmartCast(receiverArgument, smartCastType),
            candidateDescriptor, scope.ownerDescriptor
    ) == null

    private fun CallCandidateResolutionContext<*>.checkExtensionReceiver() = checkAndReport {
        val receiverParameter = candidateCall.candidateDescriptor.extensionReceiverParameter
        val receiverArgument = candidateCall.extensionReceiver
        if (receiverParameter != null && receiverArgument == null) {
            tracing.missingReceiver(candidateCall.trace, receiverParameter)
            OTHER_ERROR
        }
        else if (receiverParameter == null && receiverArgument != null) {
            tracing.noReceiverAllowed(candidateCall.trace)
            if (call.calleeExpression is KtSimpleNameExpression) {
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
        val dispatchReceiver = candidateCall.dispatchReceiver
        if (dispatchReceiver != null) {
            var nestedClass: ClassDescriptor? = null
            if (candidateDescriptor is ClassConstructorDescriptor
                && DescriptorUtils.isStaticNestedClass(candidateDescriptor.containingDeclaration)
            ) {
                nestedClass = candidateDescriptor.containingDeclaration
            }
            else if (candidateDescriptor is FakeCallableDescriptorForObject) {
                nestedClass = candidateDescriptor.getReferencedObject()
            }
            if (nestedClass != null) {
                tracing.nestedClassAccessViaInstanceReference(trace, nestedClass, candidateCall.explicitReceiverKind)
                return@checkAndReport OTHER_ERROR
            }
        }

        assert((dispatchReceiver != null) == (candidateCall.resultingDescriptor.dispatchReceiverParameter != null)) {
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
        if (context.call.explicitReceiver != null || context.call.dispatchReceiver != null) return true

        val candidateThis = getDeclaringClass(context.candidateCall.candidateDescriptor)
        if (candidateThis == null || candidateThis.kind.isSingleton) return true

        return DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, context.call.callElement, candidateThis)
    }

    private fun CallCandidateResolutionContext<*>.checkAbstractAndSuper() = check {
        val descriptor = candidateDescriptor
        val expression = candidateCall.call.calleeExpression

        if (expression is KtSimpleNameExpression) {
            // 'B' in 'class A: B()' is JetConstructorCalleeExpression
            if (descriptor is ConstructorDescriptor) {
                val modality = descriptor.constructedClass.modality
                if (modality == Modality.ABSTRACT) {
                    tracing.instantiationOfAbstractClass(trace)
                }
            }
        }

        val superDispatchReceiver = getReceiverSuper(candidateCall.dispatchReceiver)
        if (superDispatchReceiver != null) {
            if (descriptor is MemberDescriptor && descriptor.modality == Modality.ABSTRACT) {
                tracing.abstractSuperCall(trace)
                candidateCall.addStatus(OTHER_ERROR)
            }
        }

        // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
        // See TaskPrioritizer for more
        val superExtensionReceiver = getReceiverSuper(candidateCall.extensionReceiver)
        if (superExtensionReceiver != null) {
            trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(superExtensionReceiver, superExtensionReceiver.text))
            candidateCall.addStatus(OTHER_ERROR)
        }
    }

    private fun CallCandidateResolutionContext<*>.checkConstructedExpandedType() = check {
        val descriptor = candidateDescriptor

        if (descriptor is TypeAliasConstructorDescriptor) {
            if (descriptor.returnType.containsTypeProjectionsInTopLevelArguments()) {
                trace.report(EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED.on(call.callElement, descriptor.returnType))
                candidateCall.addStatus(OTHER_ERROR)
            }
        }
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
            resolveFunctionArgumentBodies: ResolveArgumentsMode
    ): ValueArgumentsCheckingResult {
        var resultStatus = SUCCESS
        val argumentTypes = Lists.newArrayList<KotlinType>()
        val infoForArguments = candidateCall.dataFlowInfoForArguments
        for ((parameterDescriptor, resolvedArgument) in candidateCall.valueArguments) {
            for (argument in resolvedArgument.arguments) {
                val expression = argument.getArgumentExpression() ?: continue

                val expectedType = getEffectiveExpectedType(parameterDescriptor, argument)

                val newContext = context.replaceDataFlowInfo(infoForArguments.getInfo(argument)).replaceExpectedType(expectedType)
                val typeInfoForCall = argumentTypeResolver.getArgumentTypeInfo(expression, newContext, resolveFunctionArgumentBodies)
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
                            resultStatus = tryNotNullableArgument(type, expectedType) ?: OTHER_ERROR
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
                        val smartCastResult = SmartCastManager.checkAndRecordPossibleCast(dataFlowValue, expectedType, expression, context,
                                                                                          call = null, recordExpressionType = false)
                        if (smartCastResult == null || !smartCastResult.isCorrect) {
                            context.trace.report(Errors.SPREAD_OF_NULLABLE.on(spreadElement))
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
            context: ResolutionContext<*>
    ): KotlinType? {
        val receiverToCast = ExpressionReceiver.create(KtPsiUtil.safeDeparenthesize(expression), actualType, context.trace.bindingContext)
        val variants = smartCastManager.getSmartCastVariantsExcludingReceiver(context, receiverToCast)
        return variants.firstOrNull { possibleType ->
            KotlinTypeChecker.DEFAULT.isSubtypeOf(possibleType, expectedType)
        }
    }

    private fun tryNotNullableArgument(argumentType: KotlinType, parameterType: KotlinType): ResolutionStatus? {
        if (!argumentType.isMarkedNullable || parameterType.isMarkedNullable) return null

        val notNullableArgumentType = argumentType.makeNotNullable()
        val isApplicable = ArgumentTypeResolver.isSubtypeOfForArgumentType(notNullableArgumentType, parameterType)
        return if (isApplicable) NULLABLE_ARGUMENT_TYPE_MISMATCH else null
    }

    private fun CallCandidateResolutionContext<*>.checkReceiverTypeError(): Unit = check {
        val extensionReceiver = candidateDescriptor.extensionReceiverParameter
        val dispatchReceiver = candidateDescriptor.dispatchReceiverParameter

        // For the expressions like '42.(f)()' where f: String.() -> Unit we'd like to generate a type mismatch error on '1',
        // not to throw away the candidate, so the following check is skipped.
        if (!isInvokeCallOnExpressionWithBothReceivers(call)) {
            checkReceiverTypeError(extensionReceiver, candidateCall.extensionReceiver)
        }
        checkReceiverTypeError(dispatchReceiver, candidateCall.dispatchReceiver)
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
                candidateCall.resultingDescriptor.extensionReceiverParameter,
                candidateCall.extensionReceiver,
                candidateCall.explicitReceiverKind.isExtensionReceiver,
                implicitInvokeCheck = false, isDispatchReceiver = false
        ))

        resultStatus = resultStatus.combine(context.checkReceiver(
                candidateCall,
                candidateCall.resultingDescriptor.dispatchReceiverParameter, candidateCall.dispatchReceiver,
                candidateCall.explicitReceiverKind.isDispatchReceiver,
                // for the invocation 'foo(1)' where foo is a variable of function type we should mark 'foo' if there is unsafe call error
                implicitInvokeCheck = context.call is CallForImplicitInvoke,
                isDispatchReceiver = true
        ))

        if (!context.isDebuggerContext
                && candidateCall.dispatchReceiver != null
                // Do not report error if it's already reported when checked without receiver
                && context.isCandidateVisible(receiverArgument = Visibilities.ALWAYS_SUITABLE_RECEIVER, smartCastType = null)) {
            resultStatus = resultStatus.combine(
                    context.checkVisibilityWithDispatchReceiver(
                            candidateCall.dispatchReceiver, candidateCall.smartCastDispatchReceiverType))
        }

        return resultStatus
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.checkReceiver(
            candidateCall: MutableResolvedCall<D>,
            receiverParameter: ReceiverParameterDescriptor?,
            receiverArgument: ReceiverValue?,
            isExplicitReceiver: Boolean,
            implicitInvokeCheck: Boolean,
            isDispatchReceiver: Boolean
    ): ResolutionStatus {
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
        val notNullReceiverExpected = !ArgumentTypeResolver.isSubtypeOfForArgumentType(receiverArgument.type, expectedReceiverParameterType)
        val smartCastNeeded =
                notNullReceiverExpected || !isCandidateVisibleOrExtensionReceiver(receiverArgument, null, isDispatchReceiver)
        var reportUnsafeCall = false

        var nullableImplicitInvokeReceiver = false
        var receiverArgumentType = receiverArgument.type
        if (implicitInvokeCheck && call is CallForImplicitInvoke && call.isSafeCall()) {
            val outerCallReceiver = call.outerCall.explicitReceiver
            if (outerCallReceiver != call.explicitReceiver && outerCallReceiver is ReceiverValue) {
                val outerReceiverDataFlowValue = DataFlowValueFactory.createDataFlowValue(outerCallReceiver, this)
                val outerReceiverNullability = dataFlowInfo.getStableNullability(outerReceiverDataFlowValue)
                if (outerReceiverNullability.canBeNull() && !TypeUtils.isNullableType(expectedReceiverParameterType)) {
                    nullableImplicitInvokeReceiver = true
                    receiverArgumentType = TypeUtils.makeNullable(receiverArgumentType)
                }
            }
        }

        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverArgument, this)
        val nullability = dataFlowInfo.getStableNullability(dataFlowValue)
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
                    dataFlowValue, expectedReceiverParameterType,
                    { possibleSmartCast -> isCandidateVisibleOrExtensionReceiver(receiverArgument, possibleSmartCast, isDispatchReceiver) },
                    expression, this, candidateCall.call, recordExpressionType = true
            )

            if (smartCastResult == null) {
                if (notNullReceiverExpected) {
                    reportUnsafeCall = true
                }
            }
            else {
                if (isDispatchReceiver) {
                    candidateCall.setSmartCastDispatchReceiverType(smartCastResult.resultType)
                }
                else {
                    candidateCall.updateExtensionReceiverWithSmartCastIfNeeded(smartCastResult.resultType)
                }
                if (!smartCastResult.isCorrect) {
                    // Error about unstable smart cast reported within checkAndRecordPossibleCast
                    return UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR
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

    private fun CallCandidateResolutionContext<*>.checkGenericBoundsInAFunctionCall(
            ktTypeArguments: List<KtTypeProjection>,
            typeArguments: List<KotlinType>,
            functionDescriptor: CallableDescriptor,
            substitutor: TypeSubstitutor,
            trace: BindingTrace
    ) {
        if (functionDescriptor is TypeAliasConstructorDescriptor) {
            checkGenericBoundsInTypeAliasConstructorCall(ktTypeArguments, functionDescriptor, substitutor, trace)
            return
        }

        val typeParameters = functionDescriptor.typeParameters
        for (i in 0..Math.min(typeParameters.size, ktTypeArguments.size) - 1) {
            val typeParameterDescriptor = typeParameters[i]
            val typeArgument = typeArguments[i]
            val typeReference = ktTypeArguments[i].typeReference
            if (typeReference != null) {
                DescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace)
            }
        }
    }

    private class TypeAliasSingleStepExpansionReportStrategy(
            private val callElement: KtElement,
            typeAlias: TypeAliasDescriptor,
            ktTypeArguments: List<KtTypeProjection>,
            private val trace: BindingTrace
    ) : TypeAliasExpansionReportStrategy {
        init {
            assert(!typeAlias.expandedType.isError) { "Incorrect type alias: $typeAlias" }
        }

        private val argumentsMapping = typeAlias.declaredTypeParameters.zip(ktTypeArguments).toMap()

        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {
            // can't happen in single-step expansion
        }

        override fun conflictingProjection(typeAlias: TypeAliasDescriptor, typeParameter: TypeParameterDescriptor?, substitutedArgument: KotlinType) {
            // can't happen in single-step expansion
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {
            // can't happen in single-step expansion
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {
            // can't happen in single-step expansion
        }

        override fun boundsViolationInSubstitution(bound: KotlinType, unsubstitutedArgument: KotlinType, argument: KotlinType, typeParameter: TypeParameterDescriptor) {
            val descriptorForUnsubstitutedArgument = unsubstitutedArgument.constructor.declarationDescriptor
            val argumentElement = argumentsMapping[descriptorForUnsubstitutedArgument]
            val argumentTypeReferenceElement = argumentElement?.typeReference
            if (argumentTypeReferenceElement != null) {
                trace.report(UPPER_BOUND_VIOLATED.on(argumentTypeReferenceElement, bound, argument))
            }
            else {
                trace.report(UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION.on(callElement, bound, argument, typeParameter))
            }
        }
    }

    private fun CallCandidateResolutionContext<*>.checkGenericBoundsInTypeAliasConstructorCall(
            ktTypeArguments: List<KtTypeProjection>,
            typeAliasConstructorDescriptor: TypeAliasConstructorDescriptor,
            typeAliasParametersSubstitutor: TypeSubstitutor,
            trace: BindingTrace
    ) {
        val substitutedType = typeAliasParametersSubstitutor.substitute(typeAliasConstructorDescriptor.returnType, Variance.INVARIANT)!!
        val boundsSubstitutor = TypeSubstitutor.create(substitutedType)

        val typeAliasDescriptor = typeAliasConstructorDescriptor.containingDeclaration

        val unsubstitutedType = typeAliasDescriptor.expandedType
        if (unsubstitutedType.isError) return

        val reportStrategy = TypeAliasSingleStepExpansionReportStrategy(call.callElement, typeAliasDescriptor, ktTypeArguments, trace)

        // TODO refactor TypeResolver
        //  - perform full type alias expansion
        //  - provide type alias expansion stack in diagnostics

        checkTypeInTypeAliasSubstitutionRec(reportStrategy, unsubstitutedType, typeAliasParametersSubstitutor, boundsSubstitutor)
    }


    private fun checkTypeInTypeAliasSubstitutionRec(
            reportStrategy: TypeAliasExpansionReportStrategy,
            unsubstitutedType: KotlinType,
            typeAliasParametersSubstitutor: TypeSubstitutor,
            boundsSubstitutor: TypeSubstitutor
    ) {
        // TODO refactor TypeResolver
        val typeParameters = unsubstitutedType.constructor.parameters

        // TODO do not perform substitution for type arguments multiple times
        val substitutedTypeArguments = typeAliasParametersSubstitutor.safeSubstitute(unsubstitutedType, Variance.INVARIANT).arguments

        for (i in 0 ..Math.min(typeParameters.size, substitutedTypeArguments.size) - 1) {
            val substitutedTypeProjection = substitutedTypeArguments[i]
            if (substitutedTypeProjection.isStarProjection) continue

            val typeParameter = typeParameters[i]
            val substitutedTypeArgument = substitutedTypeProjection.type
            val unsubstitutedTypeArgument = unsubstitutedType.arguments[i].type
            DescriptorResolver.checkBoundsInTypeAlias(reportStrategy, unsubstitutedTypeArgument, substitutedTypeArgument, typeParameter, boundsSubstitutor)

            checkTypeInTypeAliasSubstitutionRec(reportStrategy, unsubstitutedTypeArgument, typeAliasParametersSubstitutor, boundsSubstitutor)
        }
    }

    private fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.shouldContinue() =
            candidateResolveMode == CandidateResolveMode.FULLY || candidateCall.status.possibleTransformToSuccess()

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.check(
            checker: CallCandidateResolutionContext<D>.() -> Unit
    ) {
        if (shouldContinue()) checker()
    }

    private inline fun <D : CallableDescriptor> CallCandidateResolutionContext<D>.checkAndReport(
            checker: CallCandidateResolutionContext<D>.() -> ResolutionStatus
    ) {
        if (shouldContinue()) {
            candidateCall.addStatus(checker())
        }
    }

    private val CallCandidateResolutionContext<*>.candidateDescriptor: CallableDescriptor
        get() = candidateCall.candidateDescriptor
}
