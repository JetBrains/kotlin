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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

open class AbstractAccessorForFunctionDescriptor(
        containingDeclaration: DeclarationDescriptor,
        name: Name
) : SimpleFunctionDescriptorImpl(containingDeclaration, null, Annotations.EMPTY,
                                 name, CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE) {

    protected fun copyTypeParameters(descriptor: FunctionDescriptor): List<TypeParameterDescriptor> = descriptor.typeParameters.map {
        val copy = TypeParameterDescriptorImpl.createForFurtherModification(
                this, it.annotations, it.isReified,
                it.variance, it.name,
                it.index, SourceElement.NO_SOURCE)
        for (upperBound in it.upperBounds) {
            copy.addUpperBound(upperBound)
        }
        copy.setInitialized()
        copy
    }

    protected fun copyValueParameters(descriptor: FunctionDescriptor): List<ValueParameterDescriptor> =
            descriptor.valueParameters.map { it.copy(this, it.name, it.index) }
}
