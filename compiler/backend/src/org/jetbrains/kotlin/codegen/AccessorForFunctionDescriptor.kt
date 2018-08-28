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

import org.jetbrains.kotlin.codegen.coroutines.INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import java.util.*

class AccessorForFunctionDescriptor(
    override val calleeDescriptor: FunctionDescriptor,
    containingDeclaration: DeclarationDescriptor,
    override val superCallTarget: ClassDescriptor?,
    private val nameSuffix: String,
    override val accessorKind: AccessorKind
) : AbstractAccessorForFunctionDescriptor(containingDeclaration, Name.identifier("access$$nameSuffix")),
    AccessorForCallableDescriptor<FunctionDescriptor> {

    init {
        initialize(
            calleeDescriptor.extensionReceiverParameter?.copy(this),
            if (calleeDescriptor is ConstructorDescriptor || calleeDescriptor.isJvmStaticInObjectOrClassOrInterface())
                null
            else
                calleeDescriptor.dispatchReceiverParameter,
            copyTypeParameters(calleeDescriptor),
            copyValueParameters(calleeDescriptor),
            calleeDescriptor.returnType,
            Modality.FINAL,
            Visibilities.LOCAL
        )

        isSuspend = calleeDescriptor.isSuspend
        if (calleeDescriptor.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION) != null) {
            userDataMap = LinkedHashMap<FunctionDescriptor.UserDataKey<*>, Any>()
            userDataMap[INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION] =
                    calleeDescriptor.getUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION)
        }
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        return AccessorForFunctionDescriptor(calleeDescriptor, newOwner, superCallTarget, nameSuffix, accessorKind)
    }
}
