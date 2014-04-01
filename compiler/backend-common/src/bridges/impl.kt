/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.bridges

import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.Modality

public fun <Signature> generateBridgesForFunctionDescriptor(
        descriptor: FunctionDescriptor,
        signature: (FunctionDescriptor) -> Signature
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandle(descriptor), { signature(it.descriptor) })
}

private data class DescriptorBasedFunctionHandle(val descriptor: FunctionDescriptor) : FunctionHandle {
    override val isDeclaration: Boolean = descriptor.getKind().isReal()
    override val isAbstract: Boolean = descriptor.getModality() == Modality.ABSTRACT

    override fun getOverridden(): Iterable<FunctionHandle> {
        return descriptor.getOverriddenDescriptors().map { DescriptorBasedFunctionHandle(it.getOriginal()) }
    }
}
