/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Lists
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.checkers.OperatorCallChecker
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.FROM_COMPLETER
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.toHandle
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallComponents
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.resultCallAtom
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tower.PSICallResolver
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.TypeUtils.noExpectedType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.createFakeExpressionOfType
import org.jetbrains.kotlin.types.expressions.FakeCallResolver
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.util.OperatorNameConventions

//TODO: check for 'operator' modifier!
class DelegatedPropertyResolver(
    private val builtIns: KotlinBuiltIns,
    private val fakeCallResolver: FakeCallResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val psiCallResolver: PSICallResolver,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    private val callComponents: KotlinCallComponents
) {

    fun resolvePropertyDelegate(
        outerDataFlowInfo: DataFlowInfo,
        property: KtProperty,
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        propertyHeaderScope: LexicalScope,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ) {
        property.getter?.let { getter ->
            if (getter.hasBody()) trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(getter))
        }
        property.setter?.let { setter ->
            if (setter.hasBody()) trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(setter))
        }

        val initializerScope: LexicalScope =
            if (variableDescriptor is PropertyDescriptor)
                ScopeUtils.makeScopeForPropertyInitializer(propertyHeaderScope, variableDescriptor)
            else propertyHeaderScope

        val byExpressionType = resolveDelegateExpression(
            delegateExpression, property, variableDescriptor, initializerScope, trace, outerDataFlowInfo, inferenceSession
        )

        resolveProvideDelegateMethod(
            variableDescriptor, delegateExpression, byExpressionType, trace, initializerScope, outerDataFlowInfo, inferenceSession
        )
        val delegateType = getResolvedDelegateType(variableDescriptor, delegateExpression, byExpressionType, trace)

        resolveGetValueMethod(
            variableDescriptor, delegateExpression, delegateType, trace, initializerScope, outerDataFlowInfo, inferenceSession
        )
        if (property.isVar) {
            resolveSetValueMethod(variableDescriptor, delegateExpression, delegateType, trace, initializerScope, outerDataFlowInfo)
        }
    }

    private fun getResolvedDelegateType(
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        byExpressionType: KotlinType,
        trace: BindingTrace
    ): KotlinType {
        val provideDelegateResolvedCall = trace.bindingContext.get(PROVIDE_DELEGATE_RESOLVED_CALL, variableDescriptor)
        if (provideDelegateResolvedCall != null) {
            return provideDelegateResolvedCall.resultingDescriptor.returnType
                ?: throw AssertionError("No return type fore 'provideDelegate' of ${delegateExpression.text}")
        }
        return byExpressionType
    }

    fun getGetValueMethodReturnType(
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        byExpressionType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): KotlinType? {
        resolveProvideDelegateMethod(
            variableDescriptor, delegateExpression, byExpressionType, trace, initializerScope, dataFlowInfo, inferenceSession
        )
        val delegateType = getResolvedDelegateType(variableDescriptor, delegateExpression, byExpressionType, trace)
        resolveGetSetValueMethod(variableDescriptor, delegateExpression, delegateType, trace, initializerScope, dataFlowInfo, true)

        val resolvedCall = trace.bindingContext.get(DELEGATED_PROPERTY_RESOLVED_CALL, variableDescriptor.getter)
        return resolvedCall?.resultingDescriptor?.returnType
    }

    private val isOperatorProvideDelegateSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.OperatorProvideDelegate)

    private fun resolveGetValueMethod(
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ) {
        val returnType = getGetValueMethodReturnType(
            variableDescriptor, delegateExpression, delegateType, trace, initializerScope, dataFlowInfo, inferenceSession
        )
        val propertyType = variableDescriptor.type

        /* Do not check return type of get() method of delegate for properties with DeferredType because property type is taken from it */
        if (propertyType !is DeferredType && returnType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(returnType, propertyType)) {
            val call = trace.bindingContext.get(DELEGATED_PROPERTY_CALL, variableDescriptor.getter)
                ?: throw AssertionError("Call should exists for ${variableDescriptor.getter}")
            trace.report(
                DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH.on(
                    delegateExpression, renderCall(call, trace.bindingContext), variableDescriptor.type, returnType
                )
            )
        }
    }

    private fun resolveSetValueMethod(
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo
    ) {
        resolveGetSetValueMethod(
            variableDescriptor, delegateExpression, delegateType, trace,
            initializerScope, dataFlowInfo, false
        )
    }

    private fun KtPsiFactory.createExpressionForProperty(): KtExpression {
        return createExpression("null as ${StandardNames.FqNames.kPropertyFqName.asString()}<*>")
    }

    /* Resolve getValue() or setValue() methods from delegate */
    private fun resolveGetSetValueMethod(
        propertyDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        isGet: Boolean
    ) {
        val accessor = (if (isGet) propertyDescriptor.getter else propertyDescriptor.setter)
            ?: throw AssertionError("Delegated property should have getter/setter $propertyDescriptor ${delegateExpression.text}")

        if (trace.bindingContext.get(DELEGATED_PROPERTY_CALL, accessor) != null) return

        val functionResults = getGetSetValueMethod(
            propertyDescriptor, delegateExpression, delegateType, trace, initializerScope, dataFlowInfo,
            isGet = isGet, isComplete = true
        )

        if (functionResults.isSuccess) {
            recordDelegateOperatorResults(functionResults, propertyDescriptor, accessor, trace)
        } else {
            reportGetSetValueResolutionError(functionResults, accessor, delegateExpression, delegateType, trace, isGet)
        }
    }

    private fun recordDelegateOperatorResults(
        result: OverloadResolutionResults<FunctionDescriptor>,
        propertyDescriptor: VariableDescriptorWithAccessors,
        accessor: VariableAccessorDescriptor,
        trace: BindingTrace
    ) {
        val resultingDescriptor = result.resultingDescriptor
        val resultingCall = result.resultingCall

        if (!resultingDescriptor.isOperator) {
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor)
            if (declaration is KtProperty) {
                val delegate = declaration.delegate
                if (delegate != null) {
                    val byKeyword = delegate.byKeywordNode.psi
                    OperatorCallChecker.report(byKeyword, resultingDescriptor, trace)
                }
            }
        }

        trace.record(DELEGATED_PROPERTY_RESOLVED_CALL, accessor, resultingCall)
    }

    private fun reportGetSetValueResolutionError(
        result: OverloadResolutionResults<FunctionDescriptor>,
        accessor: VariableAccessorDescriptor,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        isGet: Boolean
    ) {
        val call = trace.bindingContext.get(DELEGATED_PROPERTY_CALL, accessor)
            ?: throw AssertionError("'getDelegatedPropertyConventionMethod' didn't record a call")

        val errorReportedForCandidate = reportDelegateErrorIfCandidateExists(trace, call, result, delegateExpression)
        if (!errorReportedForCandidate) {
            reportDelegateFunctionMissing(call, delegateExpression, delegateType, trace, isGet)
        }
    }

    private fun reportDelegateFunctionMissing(
        delegateOperatorCall: Call,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        isGet: Boolean
    ) {
        val expectedFunction = renderCall(delegateOperatorCall, trace.bindingContext)
        val delegateKind = if (isGet) "delegate" else "delegate for var (read-write property)"
        trace.report(DELEGATE_SPECIAL_FUNCTION_MISSING.on(delegateExpression, expectedFunction, delegateType, delegateKind))
    }

    private fun reportDelegateErrorIfCandidateExists(
        trace: BindingTrace,
        delegateOperatorCall: Call,
        delegateOperatorResults: OverloadResolutionResults<FunctionDescriptor>,
        delegateExpression: KtExpression
    ): Boolean {
        val resolutionErrorFactory = when {
            delegateOperatorResults.isSingleResult ||
                    delegateOperatorResults.isIncomplete ||
                    delegateOperatorResults.resultCode == OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES ->
                DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE

            delegateOperatorResults.isAmbiguity -> DELEGATE_SPECIAL_FUNCTION_AMBIGUITY

            else -> null
        }

        resolutionErrorFactory?.let {
            val expectedFunction = renderCall(delegateOperatorCall, trace.bindingContext)
            trace.report(it.on(delegateExpression, expectedFunction, delegateOperatorResults.resultingCalls))
        }

        return resolutionErrorFactory != null
    }

    private fun resolveProvideDelegateMethod(
        propertyDescriptor: VariableDescriptorWithAccessors,
        byExpression: KtExpression,
        byExpressionType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ) {
        if (!isOperatorProvideDelegateSupported) return
        if (trace.bindingContext.get(PROVIDE_DELEGATE_CALL, propertyDescriptor) != null) return

        val traceForProvideDelegate = TemporaryBindingTrace.create(trace, "trace to resolve provideDelegate method")

        val provideDelegateResults = getProvideDelegateMethod(
            propertyDescriptor, byExpression, byExpressionType,
            traceForProvideDelegate, initializerScope, dataFlowInfo, inferenceSession
        )
        if (!provideDelegateResults.isSuccess) {
            val call = traceForProvideDelegate.bindingContext.get(PROVIDE_DELEGATE_CALL, propertyDescriptor)
                ?: throw AssertionError("'getDelegatedPropertyConventionMethod' didn't record a call")
            val shouldCommitTrace = reportDelegateErrorIfCandidateExists(
                traceForProvideDelegate, call, provideDelegateResults, byExpression
            )

            if (shouldCommitTrace) {
                traceForProvideDelegate.commit()
            }

            return
        }

        traceForProvideDelegate.commit()

        val resultingDescriptor = provideDelegateResults.resultingDescriptor
        if (!resultingDescriptor.isOperator) {
            // TODO resolved 'provideDelegate' function, which is not an operator - warning?
            return
        }

        val resultingCall = provideDelegateResults.resultingCall
        trace.record(PROVIDE_DELEGATE_RESOLVED_CALL, propertyDescriptor, resultingCall)
    }

    private fun getGetSetValueMethod(
        propertyDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace,
        scopeForDelegate: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        isGet: Boolean,
        isComplete: Boolean,
        knownReceiver: ExpressionReceiver? = null,
        knownContext: ExpressionTypingContext? = null
    ): OverloadResolutionResults<FunctionDescriptor> {
        val delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(scopeForDelegate, propertyDescriptor)

        val accessor = (if (isGet) propertyDescriptor.getter else propertyDescriptor.setter)
            ?: throw AssertionError("Delegated property should have getter/setter $propertyDescriptor ${delegateExpression.text}")

        val expectedType = if (isComplete && isGet && propertyDescriptor.type !is DeferredType)
            propertyDescriptor.type
        else
            NO_EXPECTED_TYPE

        val context =
            knownContext ?: ExpressionTypingContext.newContext(
                trace, delegateFunctionsScope, dataFlowInfo, expectedType, languageVersionSettings, dataFlowValueFactory
            )

        val hasThis = propertyDescriptor.extensionReceiverParameter != null || propertyDescriptor.dispatchReceiverParameter != null

        val arguments = Lists.newArrayList<KtExpression>()
        val psiFactory = KtPsiFactory(delegateExpression, markGenerated = false)
        arguments.add(psiFactory.createExpression(if (hasThis) "this" else "null"))
        arguments.add(psiFactory.createExpressionForProperty())

        if (!isGet) {
            val fakeArgument = createFakeExpressionOfType(
                delegateExpression.project, trace,
                "fakeArgument${arguments.size}",
                propertyDescriptor.type
            ) as KtReferenceExpression
            arguments.add(fakeArgument)
            val valueParameters = accessor.valueParameters
            trace.record(REFERENCE_TARGET, fakeArgument, valueParameters[0])
        }

        val functionName = if (isGet) OperatorNameConventions.GET_VALUE else OperatorNameConventions.SET_VALUE
        val receiver = knownReceiver ?: ExpressionReceiver.create(delegateExpression, delegateType, trace.bindingContext)

        val resolutionResult =
            fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, functionName, delegateExpression)

        trace.record(DELEGATED_PROPERTY_CALL, accessor, resolutionResult.first)

        return resolutionResult.second
    }

    private fun createReceiverForGetSetValueMethods(
        delegateExpression: KtExpression,
        delegateType: KotlinType,
        trace: BindingTrace
    ): ExpressionReceiver =
        ExpressionReceiver.create(delegateExpression, delegateType, trace.bindingContext)

    private fun createContextForGetSetValueMethods(
        propertyDescriptor: VariableDescriptorWithAccessors,
        scopeForDelegate: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        inferenceExtension: InferenceSession
    ): ExpressionTypingContext {
        val delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(scopeForDelegate, propertyDescriptor)
        return ExpressionTypingContext.newContext(
            trace, delegateFunctionsScope, dataFlowInfo, NO_EXPECTED_TYPE,
            languageVersionSettings, dataFlowValueFactory, inferenceExtension
        )
    }

    private fun createContextForProvideDelegateMethod(
        scopeForDelegate: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        inferenceExtension: InferenceSession
    ): ExpressionTypingContext {
        return ExpressionTypingContext.newContext(
            trace, scopeForDelegate, dataFlowInfo,
            NO_EXPECTED_TYPE, ContextDependency.INDEPENDENT, StatementFilter.NONE,
            languageVersionSettings, dataFlowValueFactory, inferenceExtension
        )
    }

    private fun getProvideDelegateMethod(
        propertyDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateExpressionType: KotlinType,
        trace: BindingTrace,
        initializerScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession?
    ): OverloadResolutionResults<FunctionDescriptor> {
        val context = ExpressionTypingContext.newContext(
            trace,
            initializerScope,
            dataFlowInfo,
            NO_EXPECTED_TYPE,
            languageVersionSettings,
            dataFlowValueFactory,
            inferenceSession
        )
        return getProvideDelegateMethod(propertyDescriptor, delegateExpression, delegateExpressionType, context)
    }

    private fun getProvideDelegateMethod(
        propertyDescriptor: VariableDescriptorWithAccessors,
        delegateExpression: KtExpression,
        delegateExpressionType: KotlinType,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val propertyHasReceiver = propertyDescriptor.dispatchReceiverParameter != null
        val arguments = KtPsiFactory(delegateExpression, markGenerated = false).run {
            listOf(
                createExpression(if (propertyHasReceiver) "this" else "null"),
                createExpressionForProperty()
            )
        }
        val functionName = OperatorNameConventions.PROVIDE_DELEGATE
        val receiver = ExpressionReceiver.create(delegateExpression, delegateExpressionType, context.trace.bindingContext)

        val (provideDelegateCall, provideDelegateResults) =
            fakeCallResolver.makeAndResolveFakeCallInContext(receiver, context, arguments, functionName, delegateExpression)

        if (provideDelegateResults.isSingleResult) {
            context.trace.record(DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, delegateExpression, provideDelegateCall)
        }
        context.trace.record(PROVIDE_DELEGATE_CALL, propertyDescriptor, provideDelegateCall)

        return provideDelegateResults
    }

    //TODO: diagnostics rendering does not belong here
    private fun renderCall(call: Call, context: BindingContext): String {
        val calleeExpression = call.calleeExpression
            ?: throw AssertionError("CalleeExpression should exists for fake call of convention method")

        return call.valueArguments.joinToString(
            prefix = "${calleeExpression.text}(",
            postfix = ")",
            separator = ", ",
            transform = { argument ->
                val type = context.getType(argument.getArgumentExpression()!!)!!
                DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type)
            }
        )
    }

    fun resolveDelegateExpression(
        delegateExpression: KtExpression,
        property: KtProperty,
        variableDescriptor: VariableDescriptorWithAccessors,
        scopeForDelegate: LexicalScope,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): KotlinType {
        val propertyExpectedType = if (property.typeReference != null) variableDescriptor.type else NO_EXPECTED_TYPE

        resolveWithNewInference(
            delegateExpression,
            variableDescriptor,
            scopeForDelegate,
            trace,
            dataFlowInfo,
            inferenceSession
        )?.let { return it }

        val traceToResolveDelegatedProperty = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property")
        val completer = ConstraintSystemCompleterImpl(
            property,
            propertyExpectedType,
            variableDescriptor,
            delegateExpression,
            scopeForDelegate,
            trace,
            dataFlowInfo
        )

        delegateExpression.getCalleeExpressionIfAny()?.let {
            traceToResolveDelegatedProperty.record(CONSTRAINT_SYSTEM_COMPLETER, it, completer)
        }

        val delegateType = expressionTypingServices.safeGetType(
            scopeForDelegate,
            delegateExpression,
            NO_EXPECTED_TYPE,
            dataFlowInfo,
            inferenceSession,
            traceToResolveDelegatedProperty
        )

        traceToResolveDelegatedProperty.commit({ slice, _ -> slice !== CONSTRAINT_SYSTEM_COMPLETER }, true)

        return delegateType
    }

    private fun completeNotComputedDelegateType(trace: BindingTrace, traceToResolveDelegatedProperty: TemporaryBindingTrace) {
        val ranIntoRecursionDiagnostic = traceToResolveDelegatedProperty.bindingContext.diagnostics.find {
            it.factory == TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.errorFactory
                    || it.factory == TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.warningFactory
        }
        if (ranIntoRecursionDiagnostic != null) {
            trace.report(
                TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(
                    languageVersionSettings,
                    ranIntoRecursionDiagnostic.psiElement as KtExpression
                )
            )
        }
    }

    private fun resolveWithNewInference(
        delegateExpression: KtExpression,
        variableDescriptor: VariableDescriptorWithAccessors,
        scopeForDelegate: LexicalScope,
        trace: BindingTrace,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession
    ): KotlinType? {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return null

        trace.getType(delegateExpression)?.let { return it }

        val traceToResolveDelegatedProperty = TemporaryBindingTrace.create(trace, "Trace to resolve delegated property")

        val delegateTypeInfo = expressionTypingServices.getTypeInfo(
            scopeForDelegate, delegateExpression, NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession,
            traceToResolveDelegatedProperty, false, delegateExpression, ContextDependency.DEPENDENT
        )

        var delegateType = delegateTypeInfo.type ?: run {
            completeNotComputedDelegateType(trace, traceToResolveDelegatedProperty)
            return null
        }

        var delegateDataFlow = delegateTypeInfo.dataFlowInfo

        val delegateTypeConstructor = delegateType.constructor
        if (delegateTypeConstructor is IntegerLiteralTypeConstructor)
            delegateType = delegateTypeConstructor.getApproximatedType()

        val delegateTypeForProperType = if (delegateType.isProperType()) delegateType else null

        if (languageVersionSettings.supportsFeature(LanguageFeature.OperatorProvideDelegate)) {
            val traceForProvideDelegate = TemporaryBindingTrace.create(traceToResolveDelegatedProperty, "Trace to resolve provide delegate")

            val substitutionMap: Map<UnwrappedType, UnwrappedType>? = buildSubstitutionMapOfNonFixedVariables(delegateType)
            val nonFixedVariablesToStubTypesSubstitutor =
                if (substitutionMap != null)
                    NewTypeSubstitutorByConstructorMap(substitutionMap.mapKeys { it.key.constructor })
                else
                    EmptySubstitutor

            val delegateTypeWithoutNonFixedVariables = nonFixedVariablesToStubTypesSubstitutor.safeSubstitute(delegateType.unwrap())

            val contextForProvideDelegate = createContextForProvideDelegateMethod(
                scopeForDelegate, delegateDataFlow, traceForProvideDelegate,
                InferenceSessionForExistingCandidates(substitutionMap != null, inferenceSession)
            )

            val provideDelegateResults = getProvideDelegateMethod(
                variableDescriptor, delegateExpression, delegateTypeWithoutNonFixedVariables, contextForProvideDelegate
            )

            if (provideDelegateResults.isSuccess) {
                val provideDelegateDescriptor = provideDelegateResults.resultingDescriptor
                if (provideDelegateDescriptor.isOperator) {
                    delegateType = inverseSubstitution(provideDelegateDescriptor.returnType, substitutionMap) ?: return null
                    delegateDataFlow = provideDelegateResults.resultingCall.dataFlowInfoForArguments.resultInfo
                }
                if (substitutionMap == null) {
                    traceForProvideDelegate.record(PROVIDE_DELEGATE_RESOLVED_CALL, variableDescriptor, provideDelegateResults.resultingCall)
                    traceForProvideDelegate.commit() // otherwise we have to reanalyze provideDelegate with good delegate type
                }
            }
        }
        return inferDelegateTypeFromGetSetValueMethods(
            delegateExpression, variableDescriptor, scopeForDelegate,
            traceToResolveDelegatedProperty, delegateType, delegateTypeForProperType,
            delegateDataFlow, inferenceSession
        )
    }

    private fun inverseSubstitution(type: KotlinType?, substitutionMap: Map<UnwrappedType, UnwrappedType>?): UnwrappedType? {
        if (type == null) return null
        if (substitutionMap == null) return type.unwrap()

        val invertedMap = hashMapOf<TypeConstructor, UnwrappedType>()
        for ((variable, stubType) in substitutionMap) {
            invertedMap[stubType.constructor] = variable
        }

        return NewTypeSubstitutorByConstructorMap(invertedMap).safeSubstitute(type.unwrap())
    }

    private fun buildSubstitutionMapOfNonFixedVariables(type: KotlinType): Map<UnwrappedType, UnwrappedType>? {
        // This is an exception for delegated properties that return just type variable
        if (type.constructor is NewTypeVariableConstructor) return null

        var substitutionMap: MutableMap<UnwrappedType, UnwrappedType>? = null
        type.contains { innerType ->
            val constructor = innerType.constructor
            if (constructor is NewTypeVariableConstructor) {
                if (substitutionMap == null) substitutionMap = hashMapOf()
                if (innerType !in substitutionMap!!) {
                    substitutionMap!![innerType] = StubTypeForProvideDelegateReceiver(constructor, innerType.isMarkedNullable)
                }
            }

            false
        }

        return substitutionMap
    }

    private fun inferDelegateTypeFromGetSetValueMethods(
        delegateExpression: KtExpression,
        variableDescriptor: VariableDescriptorWithAccessors,
        scopeForDelegate: LexicalScope,
        trace: TemporaryBindingTrace,
        delegateType: KotlinType,
        delegateTypeForProperType: KotlinType?,
        delegateDataFlow: DataFlowInfo,
        inferenceSession: InferenceSession
    ): UnwrappedType {
        val expectedType = if (variableDescriptor.type !is DeferredType) variableDescriptor.type.unwrap() else null
        val newInferenceSession = DelegateInferenceSession(
            variableDescriptor, expectedType, psiCallResolver,
            postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter,
            callComponents, builtIns, inferenceSession
        )

        val receiver = createReceiverForGetSetValueMethods(delegateExpression, delegateType, trace)
        val context = createContextForGetSetValueMethods(
            variableDescriptor, scopeForDelegate, delegateDataFlow, trace, newInferenceSession
        )

        fun recordResolvedDelegateOrReportError(
            result: OverloadResolutionResults<FunctionDescriptor>,
            isGet: Boolean
        ) {
            val accessor = when (isGet) {
                true -> variableDescriptor.getter
                false -> variableDescriptor.setter
            }
            requireNotNull(accessor) {
                "Delegated property should have getter/setter $variableDescriptor ${delegateExpression.text}"
            }
            if (result.isSuccess) {
                recordDelegateOperatorResults(result, variableDescriptor, accessor, trace)
            } else {
                reportGetSetValueResolutionError(result, accessor, delegateExpression, delegateType, trace, isGet)
            }
        }

        getGetSetValueMethod(
            variableDescriptor, delegateExpression, delegateType,
            trace, scopeForDelegate, delegateDataFlow,
            isGet = true, isComplete = true, knownReceiver = receiver, knownContext = context
        )

        if (variableDescriptor.isVar && variableDescriptor.returnType !is DeferredType) {
            getGetSetValueMethod(
                variableDescriptor, delegateExpression, delegateType,
                trace, scopeForDelegate, delegateDataFlow,
                isGet = false, isComplete = true, knownReceiver = receiver, knownContext = context
            )
        }

        val call = CallMaker.makeCall(delegateExpression, receiver)
        val resolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val resolutionCallbacks = psiCallResolver.createResolutionCallbacks(trace, newInferenceSession, resolutionContext)
        val resolutionResults = newInferenceSession.resolveCandidates(resolutionCallbacks)

        for ((name, isGet) in listOf(OperatorNameConventions.GET_VALUE to true, OperatorNameConventions.SET_VALUE to false)) {
            val result = resolutionResults.firstOrNull {
                it.resolutionResult.resultCallAtom()?.atom?.name == name
            }
            result?.let { recordResolvedDelegateOrReportError(it.overloadResolutionResults, isGet) }
        }

        val resolvedDelegateType = extractResolvedDelegateType(delegateExpression, trace, delegateType)
        trace.recordType(delegateExpression, delegateTypeForProperType ?: resolvedDelegateType)
        trace.commit()
        return resolvedDelegateType.unwrap()
    }

    private fun extractResolvedDelegateType(delegateExpression: KtExpression, trace: BindingTrace, delegateType: KotlinType): KotlinType {
        val call = delegateExpression.getCall(trace.bindingContext)
        val pretendReturnType = call.getResolvedCall(trace.bindingContext)?.resultingDescriptor?.returnType
        return pretendReturnType?.takeIf { it.isProperType() }
            ?: delegateType.takeIf { it.isProperType() }
            ?: ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_DELEGATION, delegateExpression.text)
    }

    private fun KotlinType.isProperType(): Boolean {
        return !contains { it.constructor is TypeVariableTypeConstructor }
    }

    private fun conventionMethodFound(results: OverloadResolutionResults<FunctionDescriptor>): Boolean =
        results.isSuccess ||
                results.isSingleResult && results.resultCode == OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH

    inner class ConstraintSystemCompleterImpl(
        val property: KtProperty,
        val expectedType: KotlinType,
        val variableDescriptor: VariableDescriptorWithAccessors,
        val delegateExpression: KtExpression,
        private val scopeForDelegate: LexicalScope,
        val trace: BindingTrace,
        val dataFlowInfo: DataFlowInfo
    ) : ConstraintSystemCompleter {
        override fun completeConstraintSystem(constraintSystem: ConstraintSystem.Builder, resolvedCall: ResolvedCall<*>) {
            val returnType = resolvedCall.candidateDescriptor.returnType ?: return

            val typeVariableSubstitutor = constraintSystem.typeVariableSubstitutors[resolvedCall.call.toHandle()]
                ?: throw AssertionError("No substitutor in the system for call: " + resolvedCall.call)

            val traceToResolveConventionMethods =
                TemporaryBindingTrace.create(trace, "Trace to resolve delegated property convention methods")

            val delegateType = getDelegateType(returnType, constraintSystem, typeVariableSubstitutor, traceToResolveConventionMethods)

            val getValueResults = getGetSetValueMethod(
                variableDescriptor, delegateExpression, delegateType, traceToResolveConventionMethods, scopeForDelegate, dataFlowInfo,
                isGet = true, isComplete = false
            )
            if (conventionMethodFound(getValueResults)) {
                val getValueDescriptor = getValueResults.resultingDescriptor
                val getValueReturnType = getValueDescriptor.returnType
                if (getValueReturnType != null && !noExpectedType(expectedType)) {
                    val returnTypeInSystem = typeVariableSubstitutor.substitute(getValueReturnType, Variance.INVARIANT)
                    if (returnTypeInSystem != null) {
                        constraintSystem.addSubtypeConstraint(returnTypeInSystem, expectedType, FROM_COMPLETER.position())
                    }
                }
                addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, getValueDescriptor)
            }
            if (!variableDescriptor.isVar) return

            // For the case: 'val v by d' (no declared type).
            // When we add a constraint for 'set' method for delegated expression 'd' we use a type of the declared variable 'v'.
            // But if the type isn't known yet, the constraint shouldn't be added (we try to infer the type of 'v' here as well).
            if (variableDescriptor.returnType is DeferredType) return

            val setValueResults = getGetSetValueMethod(
                variableDescriptor, delegateExpression, delegateType, traceToResolveConventionMethods, scopeForDelegate, dataFlowInfo,
                isGet = false, isComplete = false
            )
            if (conventionMethodFound(setValueResults)) {
                val setValueDescriptor = setValueResults.resultingDescriptor
                val setValueParameters = setValueDescriptor.valueParameters
                if (setValueParameters.size == 3) {
                    if (!noExpectedType(expectedType)) {
                        val thisParameterType = setValueParameters[2].type
                        val substitutedThisParameterType = typeVariableSubstitutor.substitute(thisParameterType, Variance.INVARIANT)
                        constraintSystem.addSubtypeConstraint(expectedType, substitutedThisParameterType, FROM_COMPLETER.position())
                    }
                    addConstraintForThisValue(constraintSystem, typeVariableSubstitutor, setValueDescriptor)
                }
            }
        }

        private fun getDelegateType(
            byExpressionType: KotlinType,
            constraintSystem: ConstraintSystem.Builder,
            typeVariableSubstitutor: TypeSubstitutor,
            traceToResolveConventionMethods: TemporaryBindingTrace
        ): KotlinType {
            if (isOperatorProvideDelegateSupported) {
                val provideDelegateResults = getProvideDelegateMethod(
                    variableDescriptor, delegateExpression, byExpressionType,
                    traceToResolveConventionMethods, scopeForDelegate,
                    dataFlowInfo, null // it's used only from the old type inference
                )
                if (conventionMethodFound(provideDelegateResults)) {
                    val provideDelegateDescriptor = provideDelegateResults.resultingDescriptor
                    val provideDelegateReturnType = provideDelegateDescriptor.returnType
                    if (provideDelegateDescriptor.isOperator) {
                        addConstraintForThisValue(
                            constraintSystem, typeVariableSubstitutor, provideDelegateDescriptor,
                            dispatchReceiverOnly = true
                        )
                        return provideDelegateReturnType
                            ?: throw AssertionError("No return type fore 'provideDelegate' of ${delegateExpression.text}")
                    }
                }
            }

            return byExpressionType
        }

        private fun addConstraintForThisValue(
            constraintSystem: ConstraintSystem.Builder,
            typeVariableSubstitutor: TypeSubstitutor,
            resultingDescriptor: FunctionDescriptor,
            dispatchReceiverOnly: Boolean = false
        ) {
            val extensionReceiver = variableDescriptor.extensionReceiverParameter
            val dispatchReceiver = variableDescriptor.dispatchReceiverParameter
            val typeOfThis = if (dispatchReceiverOnly) {
                dispatchReceiver?.type
            } else {
                extensionReceiver?.type ?: dispatchReceiver?.type
            } ?: builtIns.nullableNothingType

            val valueParameters = resultingDescriptor.valueParameters
            if (valueParameters.isEmpty()) return
            val valueParameterForThis = valueParameters[0]

            constraintSystem.addSubtypeConstraint(
                typeOfThis,
                typeVariableSubstitutor.substitute(valueParameterForThis.type, Variance.INVARIANT),
                FROM_COMPLETER.position()
            )
        }
    }
}

