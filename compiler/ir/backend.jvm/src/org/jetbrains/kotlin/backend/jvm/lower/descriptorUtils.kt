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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name

fun FunctionDescriptor.toStatic(
    newOwner: ClassOrPackageFragmentDescriptor,
    name: Name = this.name,
    dispatchReceiverClass: ClassDescriptor? = this.containingDeclaration as? ClassDescriptor
): FunctionDescriptor {
    val newFunction = SimpleFunctionDescriptorImpl.create(
        newOwner, Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.DECLARATION, this.source
    )

    var offset = 0
    val dispatchReceiver = dispatchReceiverParameter?.let {
        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
            newFunction, null, offset++, Annotations.EMPTY, Name.identifier("this"),
            dispatchReceiverClass!!.defaultType, false, false, false, null, dispatchReceiverClass.source, null
        )
    }

    val extensionReceiver = extensionReceiverParameter?.let {
        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
            newFunction, null, offset++, Annotations.EMPTY, Name.identifier("receiver"),
            it.value.type, false, false, false, null, it.source, null
        )
    }

    val valueParameters = listOfNotNull(dispatchReceiver, extensionReceiver) +
            valueParameters.map { it.copy(newFunction, it.name, it.index + offset) }

    newFunction.initialize(
        null, null, emptyList()/*TODO: type parameters*/,
        valueParameters, returnType, Modality.FINAL, Visibilities.PUBLIC
    )
    return newFunction
}

fun FunctionDescriptor.isClInit(): Boolean = this.name == InitializersLowering.clinitName