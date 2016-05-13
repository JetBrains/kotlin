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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
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
        private val variableTypeResolver: VariableTypeResolver,
        private val delegatedPropertyResolver: DelegatedPropertyResolver
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

        val propertyDescriptor = resolveLocalVariableDescriptor(scope, property, context.dataFlowInfo, context.trace)

        val delegateExpression = property.delegateExpression
        if (delegateExpression != null && propertyDescriptor is VariableDescriptorWithAccessors) {
            delegatedPropertyResolver.resolvePropertyDelegate(typingContext.dataFlowInfo,
                                                              property,
                                                              propertyDescriptor,
                                                              delegateExpression,
                                                              typingContext.scope,
                                                              typingContext.trace);
        }

        val initializer = property.initializer
        var typeInfo: KotlinTypeInfo
        if (initializer != null) {
            val outType = propertyDescriptor.getType()
            typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType))
            val dataFlowInfo = typeInfo.dataFlowInfo
            val type = typeInfo.type
            // At this moment we do not take initializer value into account if type is given for a property
            // We can comment first part of this condition to take them into account, like here: var s: String? = "xyz"
            // In this case s will be not-nullable until it is changed
            if (property.typeReference == null && type != null) {
                val variableDataFlowValue = DataFlowValueFactory.createDataFlowValueForProperty(
                        property, propertyDescriptor, context.trace.bindingContext,
                        DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor))
                val initializerDataFlowValue = DataFlowValueFactory.createDataFlowValue(initializer, type, context)
                // We cannot say here anything new about initializerDataFlowValue
                // except it has the same value as variableDataFlowValue
                typeInfo = typeInfo.replaceDataFlowInfo(dataFlowInfo.assign(variableDataFlowValue, initializerDataFlowValue))
            }
        }
        else {
            typeInfo = noTypeInfo(context)
        }

        ExpressionTypingUtils.checkVariableShadowing(context.scope, context.trace, propertyDescriptor)

        property.checkTypeReferences(context.trace)
        modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(property, propertyDescriptor)
        identifierChecker.checkDeclaration(property, context.trace)
        return Pair(typeInfo.replaceType(dataFlowAnalyzer.checkStatementType(property, context)), propertyDescriptor)
    }

    private fun resolveLocalVariableDescriptor(
            scope: LexicalScope,
            variable: KtVariableDeclaration,
            dataFlowInfo: DataFlowInfo,
            trace: BindingTrace
    ): VariableDescriptor {
        val containingDeclaration = scope.ownerDescriptor
        val result: VariableDescriptor
        val type: KotlinType
        if (KtPsiUtil.isScriptDeclaration(variable)) {
            val propertyDescriptor = PropertyDescriptorImpl.create(
                    containingDeclaration,
                    annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),
                    Modality.FINAL,
                    Visibilities.INTERNAL,
                    variable.isVar,
                    KtPsiUtil.safeName(variable.name),
                    CallableMemberDescriptor.Kind.DECLARATION,
                    variable.toSourceElement(),
                    /* lateInit = */ false,
                    /* isConst = */ false
            )
            // For a local variable the type must not be deferred
            type = variableTypeResolver.process(propertyDescriptor, scope, variable, dataFlowInfo, false, trace)

            val receiverParameter = (containingDeclaration as ScriptDescriptor).thisAsReceiverParameter
            propertyDescriptor.setType(type, emptyList<TypeParameterDescriptor>(), receiverParameter, null as? KotlinType)
            initializeWithDefaultGetterSetter(propertyDescriptor)
            trace.record(BindingContext.VARIABLE, variable, propertyDescriptor)
            result = propertyDescriptor
        }
        else {
            val variableDescriptor = resolveLocalVariableDescriptorWithType(scope, variable, null, trace)
            // For a local variable the type must not be deferred
            type = variableTypeResolver.process(variableDescriptor, scope, variable, dataFlowInfo, false, trace)
            variableDescriptor.setOutType(type)
            result = variableDescriptor
        }
        // Type annotations also should be resolved
        ForceResolveUtil.forceResolveAllContents(type.annotations)
        return result
    }

    private fun initializeWithDefaultGetterSetter(propertyDescriptor: PropertyDescriptorImpl) {
        var getter = propertyDescriptor.getter
        if (getter == null && !Visibilities.isPrivate(propertyDescriptor.visibility)) {
            getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
            getter.initialize(propertyDescriptor.type)
        }

        var setter = propertyDescriptor.setter
        if (setter == null && propertyDescriptor.isVar) {
            setter = DescriptorFactory.createDefaultSetter(propertyDescriptor, Annotations.EMPTY)
        }
        propertyDescriptor.initialize(getter, setter)
    }

    internal fun resolveLocalVariableDescriptorWithType(
            scope: LexicalScope,
            variable: KtVariableDeclaration,
            type: KotlinType?,
            trace: BindingTrace
    ): LocalVariableDescriptor {
        val hasDelegate = variable is KtProperty && variable.hasDelegate();
        val variableDescriptor = LocalVariableDescriptor(
                scope.ownerDescriptor,
                annotationResolver.resolveAnnotationsWithArguments(scope, variable.modifierList, trace),
                KtPsiUtil.safeName(variable.name),
                type,
                variable.isVar,
                hasDelegate,
                variable.toSourceElement()
        )
        trace.record(BindingContext.VARIABLE, variable, variableDescriptor)
        return variableDescriptor
    }
}