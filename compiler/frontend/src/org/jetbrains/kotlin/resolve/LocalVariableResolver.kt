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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorWithInitializerImpl
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceSession
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo

class LocalVariableResolver(
    private val modifiersChecker: ModifiersChecker,
    private val identifierChecker: IdentifierChecker,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val annotationResolver: AnnotationResolver,
    private val variableTypeAndInitializerResolver: VariableTypeAndInitializerResolver,
    private val delegatedPropertyResolver: DelegatedPropertyResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {

    fun process(
        property: KtProperty,
        typingContext: ExpressionTypingContext,
        scope: LexicalScope,
        facade: ExpressionTypingFacade
    ): Pair<KotlinTypeInfo, VariableDescriptor> {
        val context = typingContext.replaceContextDependency(ContextDependency.INDEPENDENT).replaceScope(scope)
        val receiverTypeRef = property.receiverTypeReference
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_PROPERTY.on(receiverTypeRef))
        }

        val getter = property.getter
        if (getter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_GETTER.on(getter))
        }

        val setter = property.setter
        if (setter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_SETTER.on(setter))
        }

        val propertyDescriptor =
            resolveLocalVariableDescriptor(scope, property, context.dataFlowInfo, context.inferenceSession, context.trace)

        val delegateExpression = property.delegateExpression
        if (delegateExpression != null) {
            if (!languageVersionSettings.supportsFeature(LanguageFeature.LocalDelegatedProperties)) {
                context.trace.report(
                    UNSUPPORTED_FEATURE.on(
                        property.delegate!!,
                        LanguageFeature.LocalDelegatedProperties to languageVersionSettings
                    )
                )
            }

            if (propertyDescriptor is VariableDescriptorWithAccessors) {
                delegatedPropertyResolver.resolvePropertyDelegate(
                    typingContext.dataFlowInfo,
                    property,
                    propertyDescriptor,
                    delegateExpression,
                    typingContext.scope,
                    typingContext.inferenceSession,
                    typingContext.trace
                )
                propertyDescriptor.getter?.updateAccessorFlagsFromResolvedCallForDelegatedProperty(typingContext.trace)
                propertyDescriptor.setter?.updateAccessorFlagsFromResolvedCallForDelegatedProperty(typingContext.trace)
            }
        }

        val initializer = property.initializer
        var typeInfo: KotlinTypeInfo
        if (initializer != null) {
            val outType = propertyDescriptor.type
            typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType))
            val dataFlowInfo = typeInfo.dataFlowInfo
            val type = typeInfo.type
            if (type != null) {
                val initializerDataFlowValue = dataFlowValueFactory.createDataFlowValue(initializer, type, context)
                if (!propertyDescriptor.isVar && initializerDataFlowValue.canBeBound) {
                    context.trace.record(BindingContext.BOUND_INITIALIZER_VALUE, propertyDescriptor, initializerDataFlowValue)
                }
                // At this moment we do not take initializer value into account if type is given for a property
                // We can comment this condition to take them into account, like here: var s: String? = "xyz"
                // In this case s will be not-nullable until it is changed
                if (property.typeReference == null) {
                    val variableDataFlowValue = dataFlowValueFactory.createDataFlowValueForProperty(
                        property, propertyDescriptor, context.trace.bindingContext,
                        DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor)
                    )
                    // We cannot say here anything new about initializerDataFlowValue
                    // except it has the same value as variableDataFlowValue
                    typeInfo = typeInfo.replaceDataFlowInfo(
                        dataFlowInfo.assign(
                            variableDataFlowValue, initializerDataFlowValue,
                            languageVersionSettings
                        )
                    )
                }
            }
        } else {
            typeInfo = noTypeInfo(context)
        }

        checkLocalVariableDeclaration(context, propertyDescriptor, property)

        return Pair(typeInfo.replaceType(dataFlowAnalyzer.checkStatementType(property, context)), propertyDescriptor)
    }

    private fun checkLocalVariableDeclaration(context: ExpressionTypingContext, descriptor: VariableDescriptor, ktProperty: KtProperty) {
        ExpressionTypingUtils.checkVariableShadowing(context.scope, context.trace, descriptor)

        modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(ktProperty, descriptor)
        identifierChecker.checkDeclaration(ktProperty, context.trace)

        LateinitModifierApplicabilityChecker.checkLateinitModifierApplicability(context.trace, ktProperty, descriptor)
    }

    private fun resolveLocalVariableDescriptor(
        scope: LexicalScope,
        variable: KtVariableDeclaration,
        dataFlowInfo: DataFlowInfo,
        inferenceSession: InferenceSession,
        trace: BindingTrace
    ): VariableDescriptor {
        val containingDeclaration = scope.ownerDescriptor
        val result: VariableDescriptorWithInitializerImpl
        val type: KotlinType
        if (KtPsiUtil.isScriptDeclaration(variable)) {
            val propertyDescriptor = PropertyDescriptorImpl.create(
                containingDeclaration,
                annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),
                Modality.FINAL,
                DescriptorVisibilities.INTERNAL,
                variable.isVar,
                KtPsiUtil.safeName(variable.name),
                CallableMemberDescriptor.Kind.DECLARATION,
                variable.toSourceElement(),
                /* lateInit = */ false,
                /* isConst = */ false,
                /* isExpect = */ false,
                /* isActual = */ false,
                /* isExternal = */ false,
                variable is KtProperty && variable.hasDelegate()
            )
            // For a local variable the type must not be deferred
            type = variableTypeAndInitializerResolver.resolveType(
                propertyDescriptor, scope, variable, dataFlowInfo, inferenceSession, trace, local = true
            )

            val receiverParameter = (containingDeclaration as ScriptDescriptor).thisAsReceiverParameter
            propertyDescriptor.setType(type, emptyList<TypeParameterDescriptor>(), receiverParameter, null, emptyList<ReceiverParameterDescriptor>())
            initializeWithDefaultGetterSetter(propertyDescriptor)
            trace.record(BindingContext.VARIABLE, variable, propertyDescriptor)
            result = propertyDescriptor
            if (inferenceSession is BuilderInferenceSession) {
                inferenceSession.addLocalVariable(variable)
            }
        } else {
            val variableDescriptor = resolveLocalVariableDescriptorWithType(scope, variable, null, trace)
            // For a local variable the type must not be deferred
            type = variableTypeAndInitializerResolver.resolveType(
                variableDescriptor, scope, variable, dataFlowInfo, inferenceSession, trace, local = true
            )
            variableDescriptor.setOutType(type)
            if (inferenceSession is BuilderInferenceSession) {
                inferenceSession.addLocalVariable(variable)
            }
            result = variableDescriptor
        }
        variableTypeAndInitializerResolver
            .setConstantForVariableIfNeeded(result, scope, variable, dataFlowInfo, type, inferenceSession, trace)
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.annotations)
        return result
    }

    private fun initializeWithDefaultGetterSetter(propertyDescriptor: PropertyDescriptorImpl) {
        var getter = propertyDescriptor.getter
        if (getter == null && !DescriptorVisibilities.isPrivate(propertyDescriptor.visibility)) {
            getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
            getter.initialize(propertyDescriptor.type)
        }

        var setter = propertyDescriptor.setter
        if (setter == null && propertyDescriptor.isVar) {
            setter = DescriptorFactory.createDefaultSetter(propertyDescriptor, Annotations.EMPTY, Annotations.EMPTY)
        }
        propertyDescriptor.initialize(getter, setter)
    }

    internal fun resolveLocalVariableDescriptorWithType(
        scope: LexicalScope,
        variable: KtVariableDeclaration,
        type: KotlinType?,
        trace: BindingTrace
    ): LocalVariableDescriptor {
        val hasDelegate = variable is KtProperty && variable.hasDelegate()
        val hasLateinit = variable.hasModifier(KtTokens.LATEINIT_KEYWORD)
        val variableDescriptor = LocalVariableDescriptor(
            scope.ownerDescriptor,
            annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),
            // Note, that the same code works both for common local vars and for destructuring declarations,
            // but since the first case is illegal error must be reported somewhere else
            if (variable.isSingleUnderscore)
                Name.special("<underscore local var>")
            else
                KtPsiUtil.safeName(variable.name),
            type,
            variable.isVar,
            hasDelegate,
            hasLateinit,
            variable.toSourceElement()
        )
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor)
        return variableDescriptor
    }

    private fun VariableAccessorDescriptor.updateAccessorFlagsFromResolvedCallForDelegatedProperty(trace: BindingTrace) {
        if (this is FunctionDescriptorImpl) {
            val resultingDescriptor = trace.bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, this)?.resultingDescriptor
            if (resultingDescriptor != null) {
                setSuspend(resultingDescriptor.isSuspend)
            }
        }
    }
}
