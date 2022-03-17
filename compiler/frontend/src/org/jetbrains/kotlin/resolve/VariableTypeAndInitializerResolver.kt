/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorWithInitializerImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.DescriptorResolver.transformAnonymousTypeIfNeeded
import org.jetbrains.kotlin.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor

class VariableTypeAndInitializerResolver(
    private val storageManager: StorageManager,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeResolver: TypeResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val delegatedPropertyResolver: DelegatedPropertyResolver,
    private val wrappedTypeFactory: WrappedTypeFactory,
    private val typeApproximator: TypeApproximator,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer,
    private val languageVersionSettings: LanguageVersionSettings,
    private val anonymousTypeTransformers: Iterable<DeclarationSignatureAnonymousTypeTransformer>
) {
    companion object {
        @JvmStatic
        fun getTypeForPropertyWithoutReturnType(property: String): SimpleType =
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_PROPERTY, property)
    }

    fun resolveType(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: KtVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): KotlinType {
        resolveTypeNullable(
            variableDescriptor, scopeForInitializer, variable, dataFlowInfo, inferenceSession, trace, local
        )?.let { return it }

        if (local) {
            trace.report(VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.on(variable))
        }

        return getTypeForPropertyWithoutReturnType(variableDescriptor.name.asString())
    }

    fun resolveTypeNullable(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scopeForInitializer: LexicalScope,
        variable: KtVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): KotlinType? {
        val propertyTypeRef = variable.typeReference
        return when {
            propertyTypeRef != null -> typeResolver.resolveType(scopeForInitializer, propertyTypeRef, trace, true)

            !variable.hasInitializer() && variable is KtProperty && variableDescriptor is VariableDescriptorWithAccessors &&
                    variable.hasDelegateExpression() ->
                resolveDelegatedPropertyType(
                    variable, variableDescriptor, scopeForInitializer, dataFlowInfo, inferenceSession, trace, local
                )

            variable.hasInitializer() -> when {
                !local ->
                    wrappedTypeFactory.createRecursionIntolerantDeferredType(
                        trace
                    ) {
                        PreliminaryDeclarationVisitor.createForDeclaration(
                            variable, trace,
                            expressionTypingServices.languageVersionSettings
                        )
                        val initializerType = resolveInitializerType(
                            scopeForInitializer, variable.initializer!!, dataFlowInfo, inferenceSession, trace, local
                        )
                        transformAnonymousTypeIfNeeded(
                            variableDescriptor, variable, initializerType, trace, anonymousTypeTransformers, languageVersionSettings
                        )
                    }

                else -> resolveInitializerType(scopeForInitializer, variable.initializer!!, dataFlowInfo, inferenceSession, trace, local)
            }

            else -> null
        }
    }

    fun setConstantForVariableIfNeeded(
        variableDescriptor: VariableDescriptorWithInitializerImpl,
        scope: LexicalScope,
        variable: KtVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        variableType: KotlinType,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ) {
        if (!variable.hasInitializer() || variable.isVar) return
        variableDescriptor.setCompileTimeInitializerFactory {
            storageManager.createRecursionTolerantNullableLazyValue(
                computeInitializer@{
                    if (!DescriptorUtils.shouldRecordInitializerForProperty(
                            variableDescriptor,
                            variableType
                        )) return@computeInitializer null

                    val initializer = variable.initializer
                    val initializerType =
                        expressionTypingServices.safeGetType(scope, initializer!!, variableType, dataFlowInfo, inferenceSession, trace)
                    NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(
                        variableType,
                        initializer,
                        languageVersionSettings,
                        trace,
                        constantExpressionEvaluator.module
                    )
                    val constant = constantExpressionEvaluator.evaluateExpression(initializer, trace, initializerType)
                        ?: return@computeInitializer null

                    if (constant.usesNonConstValAsConstant && variableDescriptor.isConst) {
                        trace.report(Errors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.on(initializer))
                    }

                    constant.toConstantValue(initializerType)
                },
                null
            )
        }
    }

    private fun resolveDelegatedPropertyType(
        property: KtProperty,
        variableDescriptor: VariableDescriptorWithAccessors,
        scopeForInitializer: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ) = wrappedTypeFactory.createRecursionIntolerantDeferredType(trace) {
        val delegateExpression = property.delegateExpression!!
        val type = delegatedPropertyResolver.resolveDelegateExpression(
            delegateExpression, property, variableDescriptor, scopeForInitializer, trace, dataFlowInfo, inferenceSession
        )

        val getterReturnType = delegatedPropertyResolver.getGetValueMethodReturnType(
            variableDescriptor, delegateExpression, type, trace, scopeForInitializer, dataFlowInfo, inferenceSession
        )

        val delegatedType = getterReturnType?.let { approximateType(it, local) }
            ?: ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_DELEGATION, delegateExpression.text)

        transformAnonymousTypeIfNeeded(
            variableDescriptor, property, delegatedType, trace, anonymousTypeTransformers, languageVersionSettings
        )
    }

    private fun resolveInitializerType(
        scope: LexicalScope,
        initializer: KtExpression,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace,
        local: Boolean
    ): KotlinType {
        val inferredType = expressionTypingServices.safeGetType(
            scope, initializer, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo, inferenceSession, trace
        )
        val approximatedType = approximateType(inferredType, local)
        return declarationReturnTypeSanitizer.sanitizeReturnType(approximatedType, wrappedTypeFactory, trace, languageVersionSettings)
    }

    private fun approximateType(type: KotlinType, local: Boolean): UnwrappedType =
        typeApproximator.approximateDeclarationType(type, local)
}
