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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.types.DynamicType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.types.JetType
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollector
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollectors
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.psi.JetOperationReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.ValueArgument
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression
import org.jetbrains.kotlin.psi.JetPsiUtil

object DynamicCallableDescriptors {

    platformStatic fun createDynamicDescriptorScope(call: Call, owner: DeclarationDescriptor) = object : JetScopeImpl() {
        override fun getContainingDeclaration() = owner

        override fun printScopeStructure(p: Printer) {
            p.println(javaClass.getSimpleName(), ": dynamic candidates for " + call)
        }

        override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
            if (isAugmentedAssignmentConvention(name)) return listOf()
            if (call.getCallType() == Call.CallType.INVOKE
                && call.getValueArgumentList() == null && call.getFunctionLiteralArguments().isEmpty()) {
                // this means that we are looking for "imaginary" invokes,
                // e.g. in `+d` we are looking for property "plus" with member "invoke"
                return listOf()
            }
            return listOf(createDynamicFunction(owner, name, call))
        }

        /*
         * Detects the case when name "plusAssign" is requested for "+=" call,
         * since both "plus" and "plusAssign" are resolvable on dynamic receivers,
         * we have to prefer ne of them, and prefer "plusAssign" for generality:
         * it may be called even on a val
         */
        private fun isAugmentedAssignmentConvention(name: Name): Boolean {
            val callee = call.getCalleeExpression()
            if (callee is JetOperationReferenceExpression) {
                val token = callee.getReferencedNameElementType()
                if (token in JetTokens.AUGMENTED_ASSIGNMENTS && OperatorConventions.ASSIGNMENT_OPERATIONS[token] != name) {
                    return true
                }
            }
            return false
        }

        override fun getProperties(name: Name): Collection<VariableDescriptor> {
            return if (call.getValueArgumentList() == null && call.getValueArguments().isEmpty()) {
                listOf(createDynamicProperty(owner, name, call))
            }
            else listOf()
        }
    }

    private fun createDynamicProperty(owner: DeclarationDescriptor, name: Name, call: Call): PropertyDescriptorImpl {
        val propertyDescriptor = PropertyDescriptorImpl.create(
                owner,
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PUBLIC,
                true,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        )
        propertyDescriptor.setType(
                DynamicType,
                createTypeParameters(propertyDescriptor, call),
                createDynamicDispatchReceiverParameter(propertyDescriptor),
                null: JetType?
        )

        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor)
        getter.initialize(propertyDescriptor.getType())
        val setter = DescriptorFactory.createDefaultSetter(propertyDescriptor)

        propertyDescriptor.initialize(getter, setter)

        return propertyDescriptor
    }

    private fun createDynamicFunction(owner: DeclarationDescriptor, name: Name, call: Call): SimpleFunctionDescriptorImpl {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
                owner,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        )
        functionDescriptor.initialize(
                null,
                createDynamicDispatchReceiverParameter(functionDescriptor),
                createTypeParameters(functionDescriptor, call),
                createValueParameters(functionDescriptor, call),
                DynamicType,
                Modality.FINAL,
                Visibilities.PUBLIC
        )
        return functionDescriptor
    }

    private fun createDynamicDispatchReceiverParameter(owner: CallableDescriptor): ReceiverParameterDescriptorImpl {
        return ReceiverParameterDescriptorImpl(
                owner,
                DynamicType,
                TransientReceiver(DynamicType)
        )
    }

    private fun createTypeParameters(owner: DeclarationDescriptor, call: Call): List<TypeParameterDescriptor> = call.getTypeArguments().indices.map {
        index
        ->
        TypeParameterDescriptorImpl.createWithDefaultBound(
                owner,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.identifier("T$index"),
                index
        )
    }

    private fun createValueParameters(owner: DeclarationDescriptor, call: Call): List<ValueParameterDescriptor> {

        val parameters = ArrayList<ValueParameterDescriptor>()

        fun addParameter(arg : ValueArgument, outType: JetType, varargElementType: JetType?) {
            val index = parameters.size()

            parameters.add(ValueParameterDescriptorImpl(
                    owner,
                    null,
                    index,
                    Annotations.EMPTY,
                    arg.getArgumentName()?.getReferenceExpression()?.getReferencedNameAsName() ?: Name.identifier("p$index"),
                    outType,
                    false,
                    varargElementType,
                    SourceElement.NO_SOURCE
            ))
        }

        fun getFunctionType(funLiteralExpr: JetFunctionLiteralExpression): JetType {
            val funLiteral = funLiteralExpr.getFunctionLiteral()

            val receiverType = funLiteral.getReceiverTypeReference()?.let { DynamicType }
            val parameterTypes = funLiteral.getValueParameters().map { DynamicType }

            return KotlinBuiltIns.getInstance().getFunctionType(Annotations.EMPTY, receiverType, parameterTypes, DynamicType)
        }

        for (arg in call.getValueArguments()) {
            val outType: JetType
            val varargElementType: JetType?
            var hasSpreadOperator = false

            val argExpression = JetPsiUtil.deparenthesize(arg.getArgumentExpression(), false)

            when {
                argExpression is JetFunctionLiteralExpression -> {
                    outType = getFunctionType(argExpression)
                    varargElementType = null
                }

                arg.getSpreadElement() != null -> {
                    hasSpreadOperator = true
                    outType = KotlinBuiltIns.getInstance().getArrayType(Variance.OUT_VARIANCE, DynamicType)
                    varargElementType = DynamicType
                }

                else -> {
                    outType = DynamicType
                    varargElementType = null
                }
            }

            addParameter(arg, outType, varargElementType)

            if (hasSpreadOperator) {
                for (funLiteralArg in call.getFunctionLiteralArguments()) {
                    addParameter(funLiteralArg, getFunctionType(funLiteralArg.getFunctionLiteral()), null)
                }

                break
            }
        }

        return parameters
    }
}

public fun DeclarationDescriptor.isDynamic(): Boolean {
    if (this !is CallableDescriptor) return false
    val dispatchReceiverParameter = getDispatchReceiverParameter()
    return dispatchReceiverParameter != null && dispatchReceiverParameter.getType().isDynamic()
}

class CollectorForDynamicReceivers<D: CallableDescriptor>(val delegate: CallableDescriptorCollector<D>) : CallableDescriptorCollector<D> by delegate {
    override fun getExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<D> {
        return delegate.getExtensionsByName(scope, name, bindingTrace).filter {
            it.getExtensionReceiverParameter()?.getType()?.isDynamic() ?: false
        }
    }
}

fun <D : CallableDescriptor> CallableDescriptorCollectors<D>.onlyDynamicReceivers(): CallableDescriptorCollectors<D> {
    return CallableDescriptorCollectors(this.map { CollectorForDynamicReceivers(it) })
}
