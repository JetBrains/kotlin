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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.FunctionExpressionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorResolver.getDefaultModality
import org.jetbrains.kotlin.resolve.DescriptorResolver.getDefaultVisibility
import org.jetbrains.kotlin.resolve.DescriptorUtils.getDispatchReceiverParameterIfNeeded
import org.jetbrains.kotlin.resolve.ModifiersChecker.resolveMemberModalityFromModifiers
import org.jetbrains.kotlin.resolve.ModifiersChecker.resolveVisibilityFromModifiers
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.createValueParametersForInvokeInFunctionType
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionExpression
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import java.util.*

class FunctionDescriptorResolver(
        private val typeResolver: TypeResolver,
        private val descriptorResolver: DescriptorResolver,
        private val annotationResolver: AnnotationResolver,
        private val storageManager: StorageManager,
        private val expressionTypingServices: ExpressionTypingServices,
        private val builtIns: KotlinBuiltIns,
        private val modifiersChecker: ModifiersChecker,
        private val overloadChecker: OverloadChecker
) {
    fun resolveFunctionDescriptor(
            containingDescriptor: DeclarationDescriptor,
            scope: LexicalScope,
            function: KtNamedFunction,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo
    ): SimpleFunctionDescriptor {
        if (function.name == null) trace.report(FUNCTION_DECLARATION_WITH_NO_NAME.on(function))

        return resolveFunctionDescriptor(
                SimpleFunctionDescriptorImpl::create, containingDescriptor, scope, function, trace, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE)
    }


    fun resolveFunctionExpressionDescriptor(
            containingDescriptor: DeclarationDescriptor,
            scope: LexicalScope,
            function: KtNamedFunction,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo,
            expectedFunctionType: KotlinType
    ): SimpleFunctionDescriptor = resolveFunctionDescriptor(
            ::FunctionExpressionDescriptor, containingDescriptor, scope, function, trace, dataFlowInfo, expectedFunctionType)

    private fun resolveFunctionDescriptor(
            functionConstructor: (DeclarationDescriptor, Annotations, Name, CallableMemberDescriptor.Kind, SourceElement) -> SimpleFunctionDescriptorImpl,
            containingDescriptor: DeclarationDescriptor,
            scope: LexicalScope,
            function: KtNamedFunction,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo,
            expectedFunctionType: KotlinType
    ): SimpleFunctionDescriptor {
        val functionDescriptor = functionConstructor(
                containingDescriptor,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, function.getModifierList(), trace),
                function.getNameAsSafeName(),
                CallableMemberDescriptor.Kind.DECLARATION,
                function.toSourceElement()
        )
        initializeFunctionDescriptorAndExplicitReturnType(containingDescriptor, scope, function, functionDescriptor, trace, expectedFunctionType)
        initializeFunctionReturnTypeBasedOnFunctionBody(scope, function, functionDescriptor, trace, dataFlowInfo)
        BindingContextUtils.recordFunctionDeclarationToDescriptor(trace, function, functionDescriptor)
        return functionDescriptor
    }

    private fun initializeFunctionReturnTypeBasedOnFunctionBody(
            scope: LexicalScope,
            function: KtNamedFunction,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            trace: BindingTrace,
            dataFlowInfo: DataFlowInfo
    ) {
        if (functionDescriptor.returnType != null) return
        assert(function.getTypeReference() == null) {
            "Return type must be initialized early for function: " + function.getText() + ", at: " + DiagnosticUtils.atLocation(function) }

        val returnType = if (function.hasBlockBody()) {
            builtIns.unitType
        }
        else if (function.hasBody()) {
            descriptorResolver.inferReturnTypeFromExpressionBody(storageManager, expressionTypingServices, trace, scope,
                                              dataFlowInfo, function, functionDescriptor)
        }
        else {
            ErrorUtils.createErrorType("No type, no body")
        }
        functionDescriptor.setReturnType(returnType)
    }

    fun initializeFunctionDescriptorAndExplicitReturnType(
            containingDescriptor: DeclarationDescriptor,
            scope: LexicalScope,
            function: KtFunction,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            trace: BindingTrace,
            expectedFunctionType: KotlinType
    ) {
        val headerScope = LexicalWritableScope(scope, functionDescriptor, true, null,
                                              TraceBasedLocalRedeclarationChecker(trace, overloadChecker), LexicalScopeKind.FUNCTION_HEADER)

        val typeParameterDescriptors = descriptorResolver.
                resolveTypeParametersForDescriptor(functionDescriptor, headerScope, scope, function.typeParameters, trace)
        descriptorResolver.resolveGenericBounds(function, functionDescriptor, headerScope, typeParameterDescriptors, trace)

        val receiverTypeRef = function.receiverTypeReference
        val receiverType =
                if (receiverTypeRef != null) {
                    typeResolver.resolveType(headerScope, receiverTypeRef, trace, true)
                }
                else {
                    if (function is KtFunctionLiteral) expectedFunctionType.getReceiverType() else null
                }


        val valueParameterDescriptors = createValueParameterDescriptors(function, functionDescriptor, headerScope, trace, expectedFunctionType)

        headerScope.freeze()

        val returnType = function.typeReference?.let { typeResolver.resolveType(headerScope, it, trace, true) }

        val visibility = resolveVisibilityFromModifiers(function, getDefaultVisibility(function, containingDescriptor))
        val modality = resolveMemberModalityFromModifiers(function, getDefaultModality(containingDescriptor, visibility, function.hasBody()))
        functionDescriptor.initialize(
                receiverType,
                getDispatchReceiverParameterIfNeeded(containingDescriptor),
                typeParameterDescriptors,
                valueParameterDescriptors,
                returnType,
                modality,
                visibility
        )
        functionDescriptor.isOperator = function.hasModifier(KtTokens.OPERATOR_KEYWORD)
        functionDescriptor.isInfix = function.hasModifier(KtTokens.INFIX_KEYWORD)
        functionDescriptor.isExternal = function.hasModifier(KtTokens.EXTERNAL_KEYWORD)
        functionDescriptor.isInline = function.hasModifier(KtTokens.INLINE_KEYWORD)
        functionDescriptor.isTailrec = function.hasModifier(KtTokens.TAILREC_KEYWORD)
        functionDescriptor.isSuspend = function.hasModifier(KtTokens.SUSPEND_KEYWORD)
        receiverType?.let { ForceResolveUtil.forceResolveAllContents(it.annotations) }
        for (valueParameterDescriptor in valueParameterDescriptors) {
            ForceResolveUtil.forceResolveAllContents(valueParameterDescriptor.type.annotations)
        }
    }

    private fun createValueParameterDescriptors(
            function: KtFunction,
            functionDescriptor: SimpleFunctionDescriptorImpl,
            innerScope: LexicalWritableScope,
            trace: BindingTrace,
            expectedFunctionType: KotlinType
    ): List<ValueParameterDescriptor> {
        val expectedValueParameters = expectedFunctionType.getValueParameters(functionDescriptor)
        val expectedParameterTypes = expectedValueParameters?.map { it.type.removeParameterNameAnnotation() }
        if (expectedValueParameters != null) {
            if (expectedValueParameters.size == 1 && function is KtFunctionLiteral && function.getValueParameterList() == null) {
                // it parameter for lambda
                val valueParameterDescriptor = expectedValueParameters.single()
                val it = ValueParameterDescriptorImpl(functionDescriptor, null, 0, Annotations.EMPTY, Name.identifier("it"),
                                                      expectedParameterTypes!!.single(), valueParameterDescriptor.declaresDefaultValue(),
                                                      valueParameterDescriptor.isCrossinline, valueParameterDescriptor.isNoinline,
                                                      valueParameterDescriptor.isCoroutine,
                                                      valueParameterDescriptor.varargElementType, SourceElement.NO_SOURCE)
                trace.record(BindingContext.AUTO_CREATED_IT, it)
                return listOf(it)
            }
            if (function.valueParameters.size != expectedValueParameters.size) {
                trace.report(EXPECTED_PARAMETERS_NUMBER_MISMATCH.on(function, expectedParameterTypes!!.size, expectedParameterTypes))
            }
        }

        trace.recordScope(innerScope, function.valueParameterList)

        return resolveValueParameters(
                functionDescriptor,
                innerScope,
                function.valueParameters,
                trace,
                expectedParameterTypes
        )
    }

    private fun KotlinType.removeParameterNameAnnotation(): KotlinType {
        if (this is TypeUtils.SpecialType) return this
        val parameterNameAnnotation = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.parameterName) ?: return this
        return replaceAnnotations(AnnotationsImpl(annotations.filter { it != parameterNameAnnotation }))
    }

    private fun KotlinType.functionTypeExpected() = !TypeUtils.noExpectedType(this) && isFunctionType
    private fun KotlinType.getReceiverType(): KotlinType? =
            if (functionTypeExpected()) this.getReceiverTypeFromFunctionType() else null

    private fun KotlinType.getValueParameters(owner: FunctionDescriptor): List<ValueParameterDescriptor>? =
            if (functionTypeExpected()) {
                createValueParametersForInvokeInFunctionType(owner, this.getValueParameterTypesFromFunctionType())
            }
            else null

    fun resolvePrimaryConstructorDescriptor(
            scope: LexicalScope,
            classDescriptor: ClassDescriptor,
            classElement: KtClassOrObject,
            trace: BindingTrace
    ): ClassConstructorDescriptorImpl? {
        if (classDescriptor.getKind() == ClassKind.ENUM_ENTRY || !classElement.hasPrimaryConstructor()) return null
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                true,
                classElement.getPrimaryConstructorModifierList(),
                classElement.getPrimaryConstructor() ?: classElement,
                classElement.getPrimaryConstructorParameters(),
                trace
        )
    }

    fun resolveSecondaryConstructorDescriptor(
            scope: LexicalScope,
            classDescriptor: ClassDescriptor,
            constructor: KtSecondaryConstructor,
            trace: BindingTrace
    ): ClassConstructorDescriptorImpl {
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                false,
                constructor.getModifierList(),
                constructor,
                constructor.getValueParameters(),
                trace
        )
    }

    private fun createConstructorDescriptor(
            scope: LexicalScope,
            classDescriptor: ClassDescriptor,
            isPrimary: Boolean,
            modifierList: KtModifierList?,
            declarationToTrace: KtDeclaration,
            valueParameters: List<KtParameter>,
            trace: BindingTrace
    ): ClassConstructorDescriptorImpl {
        val constructorDescriptor = ClassConstructorDescriptorImpl.create(
                classDescriptor,
                annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList, trace),
                isPrimary,
                declarationToTrace.toSourceElement()
        )
        trace.record(BindingContext.CONSTRUCTOR, declarationToTrace, constructorDescriptor)
        val parameterScope = LexicalWritableScope(
                scope,
                constructorDescriptor,
                false, null,
                TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                LexicalScopeKind.CONSTRUCTOR_HEADER
        )
        val constructor = constructorDescriptor.initialize(
                resolveValueParameters(constructorDescriptor, parameterScope, valueParameters, trace, null),
                resolveVisibilityFromModifiers(
                        modifierList,
                        DescriptorUtils.getDefaultConstructorVisibility(classDescriptor)
                )
        )
        if (DescriptorUtils.isAnnotationClass(classDescriptor)) {
            CompileTimeConstantUtils.checkConstructorParametersType(valueParameters, trace)
        }
        return constructor
    }

    private fun resolveValueParameters(
            functionDescriptor: FunctionDescriptor,
            parameterScope: LexicalWritableScope,
            valueParameters: List<KtParameter>,
            trace: BindingTrace,
            expectedParameterTypes: List<KotlinType>?
    ): List<ValueParameterDescriptor> {
        val result = ArrayList<ValueParameterDescriptor>()

        for (i in valueParameters.indices) {
            val valueParameter = valueParameters[i]
            val typeReference = valueParameter.typeReference
            val expectedType = expectedParameterTypes?.let { if (i < it.size) it[i] else null }

            val type: KotlinType
            if (typeReference != null) {
                type = typeResolver.resolveType(parameterScope, typeReference, trace, true)
                if (expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
                    if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(expectedType, type)) {
                        trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(valueParameter, expectedType))
                    }
                }
            }
            else {
                if (isFunctionLiteral(functionDescriptor) || isFunctionExpression(functionDescriptor)) {
                    val containsUninferredParameter = TypeUtils.contains(expectedType) {
                        TypeUtils.isDontCarePlaceholder(it) || ErrorUtils.isUninferredParameter(it)
                    }
                    if (expectedType == null || containsUninferredParameter) {
                        trace.report(CANNOT_INFER_PARAMETER_TYPE.on(valueParameter))
                    }

                    type = expectedType ?: TypeUtils.CANT_INFER_FUNCTION_PARAM_TYPE
                }
                else {
                    trace.report(VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.on(valueParameter))
                    type = ErrorUtils.createErrorType("Type annotation was missing for parameter ${valueParameter.nameAsSafeName}")
                }
            }

            if (functionDescriptor !is ConstructorDescriptor || !functionDescriptor.isPrimary) {
                val isConstructor = functionDescriptor is ConstructorDescriptor
                with (modifiersChecker.withTrace(trace)) {
                    checkParameterHasNoValOrVar(
                            valueParameter,
                            if (isConstructor) VAL_OR_VAR_ON_SECONDARY_CONSTRUCTOR_PARAMETER else VAL_OR_VAR_ON_FUN_PARAMETER
                    )
                }
            }

            val valueParameterDescriptor = descriptorResolver.resolveValueParameterDescriptor(parameterScope, functionDescriptor,
                                                                                              valueParameter, i, type, trace)
            parameterScope.addVariableDescriptor(valueParameterDescriptor)
            result.add(valueParameterDescriptor)
        }
        return result
    }
}
