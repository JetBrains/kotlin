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

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableNothing
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.before
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue.Kind.*
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor
import org.jetbrains.kotlin.types.isError

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
            return DataFlowValue(ExpressionIdentifierInfo(expression),
                                 type,
                                 Nullability.NOT_NULL)
        }

        if (isComplexExpression(expression)) {
            return createDataFlowValueForComplexExpression(expression, type)
        }

        val result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule)
        return DataFlowValue(if (result === IdentifierInfo.NO) ExpressionIdentifierInfo(expression) else result, type)
    }

    @JvmStatic
    fun createDataFlowValueForStableReceiver(receiver: ReceiverValue) = DataFlowValue(IdentifierInfo.Receiver(receiver), receiver.type)

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
        else -> throw UnsupportedOperationException("Unsupported receiver value: " + receiverValue::class.java.name)
    }

    @JvmStatic
    fun createDataFlowValueForProperty(
            property: KtProperty,
            variableDescriptor: VariableDescriptor,
            bindingContext: BindingContext,
            usageContainingModule: ModuleDescriptor?
    ) = DataFlowValue(IdentifierInfo.Variable(variableDescriptor,
                                              variableKind(variableDescriptor, usageContainingModule,
                                                           bindingContext, property),
                                              bindingContext[BOUND_INITIALIZER_VALUE, variableDescriptor]),
                                     variableDescriptor.type)

    private fun createDataFlowValueForComplexExpression(
            expression: KtExpression,
            type: KotlinType
    ) = DataFlowValue(ExpressionIdentifierInfo(expression, stableComplex = true), type)

    // For only ++ and -- postfix operations
    private data class PostfixIdentifierInfo(val argumentInfo: IdentifierInfo, val op: KtToken) : IdentifierInfo {
        override val kind: DataFlowValue.Kind get() = argumentInfo.kind

        override fun toString() = "$argumentInfo($op)"
    }

    class ExpressionIdentifierInfo(val expression: KtExpression, stableComplex: Boolean = false) : IdentifierInfo {

        override val kind = if (stableComplex) STABLE_COMPLEX_EXPRESSION else OTHER
        
        override fun equals(other: Any?) = other is ExpressionIdentifierInfo && expression == other.expression

        override fun hashCode() = expression.hashCode()

        override fun toString() = expression.text ?: "(empty expression)"
    }

    private fun postfix(argumentInfo: IdentifierInfo, op: KtToken) =
            if (argumentInfo == IdentifierInfo.NO) {
                IdentifierInfo.NO
            }
            else {
                PostfixIdentifierInfo(argumentInfo, op)
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
                val receiverInfo = getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclarationOrModule)
                val selectorInfo = getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclarationOrModule)

                IdentifierInfo.qualified(receiverInfo, bindingContext.getType(receiverExpression),
                                         selectorInfo, expression.operationSign === KtTokens.SAFE_ACCESS)
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
                    postfix(getIdForStableIdentifier(expression.baseExpression, bindingContext, containingDeclarationOrModule),
                            operationType)
                }
                else {
                    IdentifierInfo.NO
                }
            }
            else -> IdentifierInfo.NO
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
                val selectorInfo = IdentifierInfo.Variable(declarationDescriptor,
                                                           variableKind(declarationDescriptor, usageModuleDescriptor,
                                                                        bindingContext, simpleNameExpression),
                                                           bindingContext[BOUND_INITIALIZER_VALUE, declarationDescriptor])

                val implicitReceiver = resolvedCall?.dispatchReceiver
                if (implicitReceiver == null) {
                    selectorInfo
                }
                else {
                    val receiverInfo = getIdForImplicitReceiver(implicitReceiver, simpleNameExpression)

                    if (receiverInfo == null) {
                        selectorInfo
                    }
                    else {
                        IdentifierInfo.qualified(receiverInfo, implicitReceiver.type,
                                                 selectorInfo, resolvedCall.call.isSafeCall())
                    }
                }
            }
            is PackageViewDescriptor, is ClassDescriptor -> IdentifierInfo.PackageOrClass(declarationDescriptor)
            else -> IdentifierInfo.NO
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
            IdentifierInfo.Receiver(receiverParameter.value)
        }
        is ClassDescriptor -> IdentifierInfo.Receiver(descriptorOfThisReceiver.thisAsReceiverParameter.value)
        else -> IdentifierInfo.NO
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

        // Local variable classification: STABLE or CAPTURED
        val preliminaryVisitor = PreliminaryDeclarationVisitor.getVisitorByVariable(variableDescriptor, bindingContext)
                                 // A case when we just analyse an expression alone: counts as captured
                                 ?: return CAPTURED_VARIABLE

        // Analyze who writes variable
        // If there is no writer: stable
        val writers = preliminaryVisitor.writers(variableDescriptor)
        if (writers.isEmpty()) return STABLE_VARIABLE

        // If access element is inside closure: captured
        val variableContainingDeclaration = getVariableContainingDeclaration(variableDescriptor)
        if (isAccessedInsideClosure(variableContainingDeclaration, bindingContext, accessElement)) return CAPTURED_VARIABLE

        // Otherwise, stable iff considered position is BEFORE all writers except declarer itself
        return if (isAccessedBeforeAllClosureWriters(variableContainingDeclaration, writers, bindingContext, accessElement))
            STABLE_VARIABLE
        else
            CAPTURED_VARIABLE
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
