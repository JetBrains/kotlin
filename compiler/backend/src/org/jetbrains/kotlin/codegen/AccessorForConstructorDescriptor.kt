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
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.JetType
import java.util.*

public class AccessorForConstructorDescriptor(
        private val calleeDescriptor: ConstructorDescriptor,
        containingDeclaration: DeclarationDescriptor
) : AbstractAccessorForFunctionDescriptor(containingDeclaration, Name.special("<init>"))
    , ConstructorDescriptor
    , AccessorForCallableDescriptor<ConstructorDescriptor> {

    override fun getCalleeDescriptor(): ConstructorDescriptor = calleeDescriptor

    override fun getContainingDeclaration(): ClassDescriptor = calleeDescriptor.getContainingDeclaration()

    override fun isPrimary(): Boolean = false

    override fun getReturnType(): JetType = super<AbstractAccessorForFunctionDescriptor>.getReturnType()!!

    init {
        initialize(
                DescriptorUtils.getReceiverParameterType(getExtensionReceiverParameter()),
                calleeDescriptor.getDispatchReceiverParameter(),
                copyTypeParameters(calleeDescriptor),
                copyValueParameters(calleeDescriptor),
                calleeDescriptor.getReturnType(),
                Modality.FINAL,
                Visibilities.INTERNAL
        )
    }
}
