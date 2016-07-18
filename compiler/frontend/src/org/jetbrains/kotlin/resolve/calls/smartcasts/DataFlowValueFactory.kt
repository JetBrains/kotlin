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

package org.jetbrains.kotlin.resolve.calls.smartcasts

import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableNothing
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.*

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
object DataFlowValueFactory {

    @JvmStatic
    fun createDataFlowValue(
            expression: KtExpression,
            type: KotlinType,
            resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(expression, type, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    private fun isComplexExpression(expression: KtExpression): Boolean = when(expression) {
        is KtBlockExpression, is KtIfExpression, is KtWhenExpression -> true
        is KtBinaryExpression -> expression.operationToken === KtTokens.ELVIS
        is KtParenthesizedExpression -> {
            val deparenthesized = KtPsiUtil.deparenthesize(expression)
            deparenthesized != null && isComplexExpression(deparenthesized)
        }
        else -> false
    }

    @JvmStatic
    fun createDataFlowValue(
            expression: KtExpression,
            type: KotlinType,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue {
        if (expression is KtConstantExpression) {
            if (expression.node.elementType === KtNodeTypes.NULL) {
                return DataFlowValue.nullValue(containingDeclarationOrModule.builtIns)
            }
        }
        if (type.isError) return DataFlowValue.ERROR
        if (isNullableNothing(type)) {
            return DataFlowValue.nullValue(containingDeclarationOrModule.builtIns) // 'null' is the only inhabitant of 'Nothing?'
        }

        if (ExpressionTypingUtils.isExclExclExpression(KtPsiUtil.deparenthesize(expression))) {
            // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
            // by calling `getImmanentNullability` (as it happens below).
            //
            // But there are some problem with types built on type parameters, e.g.
            // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            return DataFlowValue(expression,
                                 type,
                                 OTHER,
                                 Nullability.NOT_NULL)
        }

        if (isComplexExpression(expression)) {
            return createDataFlowValueForComplexExpression(expression, type)
        }

        val result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule)
        return DataFlowValue(if (result === NO_IDENTIFIER_INFO) expression else result.id,
                             type,
                             result.kind,
                             type.immanentNullability)
    }

    @JvmStatic
    fun createDataFlowValueForStableReceiver(receiver: ReceiverValue): DataFlowValue {
        val type = receiver.type
        return DataFlowValue(receiver, type, STABLE_VALUE, type.immanentNullability)
    }

    @JvmStatic
    fun createDataFlowValue(
            receiverValue: ReceiverValue,
            resolutionContext: ResolutionContext<*>
    ) = createDataFlowValue(receiverValue, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    @JvmStatic
    fun createDataFlowValue(
            receiverValue: ReceiverValue,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor
    ) = when (receiverValue) {
        is TransientReceiver, is ImplicitReceiver -> createDataFlowValueForStableReceiver(receiverValue)
        is ExpressionReceiver -> createDataFlowValue(receiverValue.expression,
                                                     receiverValue.getType(),
                                                     bindingContext,
                                                     containingDeclarationOrModule)
        else -> throw UnsupportedOperationException("Unsupported receiver value: " + receiverValue.javaClass.name)
    }

    @JvmStatic
    fun createDataFlowValueForProperty(
            property: KtProperty,
            variableDescriptor: VariableDescriptor,
            bindingContext: BindingContext,
            usageContainingModule: ModuleDescriptor?
    ): DataFlowValue {
        val type = variableDescriptor.type
        return DataFlowValue(variableDescriptor, type,
                             variableKind(variableDescriptor, usageContainingModule,
                                          bindingContext, property),
                             type.immanentNullability)
    }

    private fun createDataFlowValueForComplexExpression(
            expression: KtExpression,
            type: KotlinType
    ) = DataFlowValue(expression, type, Kind.STABLE_COMPLEX_EXPRESSION, type.immanentNullability)

    private val KotlinType.immanentNullability: Nullability
        get() = if (TypeUtils.isNullableType(this)) Nullability.UNKNOWN else Nullability.NOT_NULL

    private open class IdentifierInfo internal constructor(val id: Any?, val kind: Kind, val isPackage: Boolean)

    private val NO_IDENTIFIER_INFO = object : IdentifierInfo(null, OTHER, false) {
        override fun toString() = "NO_IDENTIFIER_INFO"
    }

    private fun createInfo(id: Any, kind: Kind) = IdentifierInfo(id, kind, false)

    private fun createStableInfo(id: Any) = createInfo(id, STABLE_VALUE)

    private fun createPackageOrClassInfo(id: Any) = IdentifierInfo(id, STABLE_VALUE, true)

    private fun combineInfo(receiverInfo: IdentifierInfo?, selectorInfo: IdentifierInfo) =
            if (selectorInfo.id == null || receiverInfo === NO_IDENTIFIER_INFO) {
                NO_IDENTIFIER_INFO
            }
            else if (receiverInfo == null || receiverInfo.isPackage) {
                selectorInfo
            }
            else {
                createInfo(Pair.create<Any, Any>(receiverInfo.id, selectorInfo.id),
                           if (receiverInfo.kind.isStable()) selectorInfo.kind else OTHER)
            }

    private fun createPostfixInfo(expression: KtPostfixExpression, argumentInfo: IdentifierInfo) =
            if (argumentInfo === NO_IDENTIFIER_INFO) {
                NO_IDENTIFIER_INFO
            }
            else {
                createInfo(Pair.create<KtPostfixExpression, Any>(expression, argumentInfo.id), argumentInfo.kind)
            }

    private fun getIdForStableIdentifier(
            expression: KtExpression?,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor
    ): IdentifierInfo {
        if (expression != null) {
            val deparenthesized = KtPsiUtil.deparenthesize(expression)
            if (expression !== deparenthesized) {
                return getIdForStableIdentifier(deparenthesized, bindingContext, containingDeclarationOrModule)
            }
        }
        return when (expression) {
            is KtQualifiedExpression -> {
                val receiverExpression = expression.receiverExpression
                val selectorExpression = expression.selectorExpression
                val receiverId = getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclarationOrModule)
                val selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclarationOrModule)

                combineInfo(receiverId, selectorId)
            }
            is KtSimpleNameExpression ->
                getIdForSimpleNameExpression(expression, bindingContext, containingDeclarationOrModule)
            is KtThisExpression -> {
                val declarationDescriptor = bindingContext.get(REFERENCE_TARGET, expression.instanceReference)
                getIdForThisReceiver(declarationDescriptor)
            }
            is KtPostfixExpression -> {
                val operationType = expression.operationReference.getReferencedNameElementType()
                if (operationType === KtTokens.PLUSPLUS || operationType === KtTokens.MINUSMINUS) {
                    createPostfixInfo(expression,
                                      getIdForStableIdentifier(expression.baseExpression, bindingContext, containingDeclarationOrModule))
                }
                else {
                    NO_IDENTIFIER_INFO
                }
            }
            else -> NO_IDENTIFIER_INFO
        }
    }

    private fun getIdForSimpleNameExpression(
            simpleNameExpression: KtSimpleNameExpression,
            bindingContext: BindingContext,
            containingDeclarationOrModule: DeclarationDescriptor
    ): IdentifierInfo {
        val declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression)
        return when (declarationDescriptor) {
            is VariableDescriptor -> {
                val resolvedCall = simpleNameExpression.getResolvedCall(bindingContext)

                // todo uncomment assert
                // KT-4113
                // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
                // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
                val usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclarationOrModule)
                val receiverInfo = resolvedCall?.let { getIdForImplicitReceiver(it.dispatchReceiver, simpleNameExpression) }

                combineInfo(receiverInfo, createInfo(declarationDescriptor,
                                                     variableKind(declarationDescriptor, usageModuleDescriptor,
                                                                  bindingContext, simpleNameExpression)))
            }
            is PackageViewDescriptor, is ClassDescriptor -> createPackageOrClassInfo(declarationDescriptor)
            else -> NO_IDENTIFIER_INFO
        }
    }

    private fun getIdForImplicitReceiver(receiverValue: ReceiverValue?, expression: KtExpression?) =
            when (receiverValue) {
                is ImplicitReceiver -> getIdForThisReceiver(receiverValue.declarationDescriptor)
                is TransientReceiver ->
                    throw AssertionError("Transient receiver is implicit for an explicit expression: $expression. Receiver: $receiverValue")
                else -> null
            }

    private fun getIdForThisReceiver(descriptorOfThisReceiver: DeclarationDescriptor?) = when (descriptorOfThisReceiver) {
        is CallableDescriptor -> {
            val receiverParameter = descriptorOfThisReceiver.extensionReceiverParameter
                                    ?: error("'This' refers to the callable member without a receiver parameter: $descriptorOfThisReceiver")
            createStableInfo(receiverParameter.value)
        }
        is ClassDescriptor -> createStableInfo(descriptorOfThisReceiver.thisAsReceiverParameter.value)
        else -> NO_IDENTIFIER_INFO
    }

    private fun getVariableContainingDeclaration(variableDescriptor: VariableDescriptor): DeclarationDescriptor {
        val containingDeclarationDescriptor = variableDescriptor.containingDeclaration
        return if (containingDeclarationDescriptor is ConstructorDescriptor && containingDeclarationDescriptor.isPrimary) {
            // This code is necessary just because JetClassInitializer has no associated descriptor in trace
            // Because of it we have to use class itself instead of initializer,
            // otherwise we could not find this descriptor inside isAccessedInsideClosure below
            containingDeclarationDescriptor.containingDeclaration
        }
        else {
            containingDeclarationDescriptor
        }
    }

    private fun isAccessedInsideClosure(
            variableContainingDeclaration: DeclarationDescriptor,
            bindingContext: BindingContext,
            accessElement: KtElement
    ): Boolean {
        val parent = ControlFlowInformationProvider.getElementParentDeclaration(accessElement)
        return if (parent != null)
            // Access is at the same declaration: not in closure, lower: in closure
            variableContainingDeclaration != bindingContext.get(DECLARATION_TO_DESCRIPTOR, parent)
        else
            false
    }

    private fun isAccessedBeforeAllClosureWriters(
            variableContainingDeclaration: DeclarationDescriptor,
            writers: Set<KtDeclaration?>,
            bindingContext: BindingContext,
            accessElement: KtElement
    ): Boolean {
        // All writers should be before access element, with the exception:
        // writer which is the same with declaration site does not count
        writers.filterNotNull().forEach { writer ->
            val writerDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, writer)
            // Access is after some writer
            if (variableContainingDeclaration != writerDescriptor && !accessElement.before(writer)) {
                return false
            }
        }
        // Access is before all writers
        return true
    }

    private fun propertyKind(propertyDescriptor: PropertyDescriptor, usageModule: ModuleDescriptor?): Kind {
        if (propertyDescriptor.isVar) return MUTABLE_PROPERTY
        if (propertyDescriptor.isOverridable) return PROPERTY_WITH_GETTER
        if (!hasDefaultGetter(propertyDescriptor)) return PROPERTY_WITH_GETTER
        if (!invisibleFromOtherModules(propertyDescriptor)) {
            val declarationModule = DescriptorUtils.getContainingModule(propertyDescriptor)
            if (usageModule == null || usageModule != declarationModule) {
                return ALIEN_PUBLIC_PROPERTY
            }
        }
        return STABLE_VALUE
    }

    private fun variableKind(
            variableDescriptor: VariableDescriptor,
            usageModule: ModuleDescriptor?,
            bindingContext: BindingContext,
            accessElement: KtElement
    ): Kind {
        if (variableDescriptor is PropertyDescriptor) {
            return propertyKind(variableDescriptor, usageModule)
        }
        if (variableDescriptor !is LocalVariableDescriptor && variableDescriptor !is ParameterDescriptor) return OTHER
        if (!variableDescriptor.isVar) return STABLE_VALUE
        if (variableDescriptor is SyntheticFieldDescriptor) return MUTABLE_PROPERTY

        // Local variable classification: PREDICTABLE or UNPREDICTABLE
        val preliminaryVisitor = PreliminaryDeclarationVisitor.getVisitorByVariable(variableDescriptor, bindingContext)
                                 ?: return UNPREDICTABLE_VARIABLE
        // A case when we just analyse an expression alone: counts as unpredictable

        // Analyze who writes variable
        // If there is no writer: predictable
        val writers = preliminaryVisitor.writers(variableDescriptor)
        if (writers.isEmpty()) return PREDICTABLE_VARIABLE

        // If access element is inside closure: unpredictable
        val variableContainingDeclaration = getVariableContainingDeclaration(variableDescriptor)
        if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) return UNPREDICTABLE_VARIABLE

        // Otherwise, predictable iff considered position is BEFORE all writers except declarer itself
        return if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement))
            PREDICTABLE_VARIABLE
        else
            UNPREDICTABLE_VARIABLE
    }

    /**
     * Determines whether a variable with a given descriptor is stable or not at the given usage place.
     *
     *
     * Stable means that the variable value cannot change. The simple (non-property) variable is considered stable if it's immutable (val).
     *
     *
     * If the variable is a property, it's considered stable if it's immutable (val) AND it's final (not open) AND
     * the default getter is in use (otherwise nobody can guarantee that a getter is consistent) AND
     * (it's private OR internal OR used at the same module where it's defined).
     * The last check corresponds to a risk of changing property definition in another module, e.g. from "val" to "var".

     * @param variableDescriptor    descriptor of a considered variable
     * *
     * @param usageModule a module with a considered usage place, or null if it's not known (not recommended)
     * *
     * @return true if variable is stable, false otherwise
     */
    fun isStableValue(
            variableDescriptor: VariableDescriptor,
            usageModule: ModuleDescriptor?
    ): Boolean {
        if (variableDescriptor.isVar) return false
        return variableDescriptor !is PropertyDescriptor || propertyKind(variableDescriptor, usageModule) === STABLE_VALUE
    }

    private fun invisibleFromOtherModules(descriptor: DeclarationDescriptorWithVisibility): Boolean {
        if (Visibilities.INVISIBLE_FROM_OTHER_MODULES.contains(descriptor.visibility)) return true

        val containingDeclaration = descriptor.containingDeclaration
        return containingDeclaration is DeclarationDescriptorWithVisibility && invisibleFromOtherModules(containingDeclaration)
    }

    private fun hasDefaultGetter(propertyDescriptor: PropertyDescriptor): Boolean {
        val getter = propertyDescriptor.getter
        return getter == null || getter.isDefault
    }
}
