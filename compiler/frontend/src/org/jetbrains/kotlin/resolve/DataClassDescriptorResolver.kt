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
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.util.OperatorNameConventions

object DataClassDescriptorResolver {
    val COPY_METHOD_NAME = Name.identifier("copy")

    fun createComponentName(index: Int): Name = DataClassResolver.createComponentName(index)

    fun getComponentIndex(componentName: String): Int = DataClassResolver.getComponentIndex(componentName)

    fun isComponentLike(name: Name): Boolean = DataClassResolver.isComponentLike(name)

    fun createComponentFunctionDescriptor(
        parameterIndex: Int,
        property: PropertyDescriptor,
        parameter: ValueParameterDescriptor,
        classDescriptor: ClassDescriptor,
        trace: BindingTrace
    ): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            classDescriptor,
            Annotations.EMPTY,
            createComponentName(parameterIndex),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            parameter.source
        )

        functionDescriptor.initialize(
            null,
            classDescriptor.thisAsReceiverParameter,
            emptyList<ReceiverParameterDescriptor>(),
            emptyList<TypeParameterDescriptor>(),
            emptyList<ValueParameterDescriptor>(),
            property.type,
            Modality.FINAL,
            property.visibility
        )
        functionDescriptor.isOperator = true

        trace.record(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter, functionDescriptor)
        return functionDescriptor
    }

    fun createCopyFunctionDescriptor(
        constructorParameters: Collection<ValueParameterDescriptor>,
        classDescriptor: ClassDescriptor,
        trace: BindingTrace
    ): SimpleFunctionDescriptor {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
            classDescriptor,
            Annotations.EMPTY,
            COPY_METHOD_NAME,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            classDescriptor.source
        )

        val parameterDescriptors = arrayListOf<ValueParameterDescriptor>()

        for (parameter in constructorParameters) {
            val propertyDescriptor = trace.bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
            // If a parameter doesn't have the corresponding property, it must not have a default value in the 'copy' function
            val declaresDefaultValue = propertyDescriptor != null
            val parameterDescriptor = ValueParameterDescriptorImpl(
                functionDescriptor, null, parameter.index, parameter.annotations, parameter.name, parameter.type, declaresDefaultValue,
                parameter.isCrossinline, parameter.isNoinline, parameter.varargElementType, parameter.source
            )
            parameterDescriptors.add(parameterDescriptor)
            if (declaresDefaultValue) {
                trace.record(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameterDescriptor, propertyDescriptor)
            }
        }

        functionDescriptor.initialize(
            null,
            classDescriptor.thisAsReceiverParameter,
            emptyList<ReceiverParameterDescriptor>(),
            emptyList<TypeParameterDescriptor>(),
            parameterDescriptors,
            classDescriptor.defaultType,
            Modality.FINAL,
            DescriptorVisibilities.PUBLIC
        )

        trace.record(BindingContext.DATA_CLASS_COPY_FUNCTION, classDescriptor, functionDescriptor)
        return functionDescriptor
    }
}
