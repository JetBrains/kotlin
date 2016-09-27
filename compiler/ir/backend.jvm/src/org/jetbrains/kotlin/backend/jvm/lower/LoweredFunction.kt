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

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name

interface LoweredFunction: FunctionDescriptor

class LoweredFunctionImpl(containingDeclaration: DeclarationDescriptor,
                          original: FunctionDescriptor,
                          annotations: Annotations,
                          name: Name,
                          kind: CallableMemberDescriptor.Kind,
                          source: SourceElement) :
        FunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source), LoweredFunction {
    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return super.getDispatchReceiverParameter()
    }

    override fun createSubstitutedCopy(newOwner: DeclarationDescriptor, original: FunctionDescriptor?, kind: CallableMemberDescriptor.Kind, newName: Name?, annotations: Annotations, source: SourceElement): FunctionDescriptorImpl {
        TODO("not implemented")
    }
}