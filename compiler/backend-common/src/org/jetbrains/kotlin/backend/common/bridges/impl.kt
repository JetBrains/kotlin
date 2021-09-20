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

package org.jetbrains.kotlin.backend.common.bridges

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.util.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.descriptorUtil.isTypeRefinementEnabled
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.util.findImplementationFromInterface
import org.jetbrains.kotlin.util.findInterfaceImplementation

fun <Signature> generateBridgesForFunctionDescriptor(
    descriptor: FunctionDescriptor,
    signature: (FunctionDescriptor) -> Signature
): Set<Bridge<Signature, DescriptorBasedFunctionHandle>> {
    return generateBridges(DescriptorBasedFunctionHandle(descriptor), { signature(it.descriptor) })
}

/**
 * An implementation of FunctionHandle based on descriptors.
 *
 * This implementation workarounds a minor inconvenience in descriptor hierarchy regarding traits with implementations.
 * Consider the following hierarchy:
 *
 * trait A { fun foo() = 42 }
 * class B : A
 *
 * In terms of descriptors, we'll have a declaration in trait A with modality=OPEN and a fake override in class B with modality=OPEN.
 * For the purposes of bridge generation though, it's much easier to "move" all implementations out of traits into their child classes,
 * i.e. treat the function in A as a declaration with modality=ABSTRACT and a function in B as a _declaration_ with modality=OPEN.
 *
 * This provides us with the nice invariant that all implementations (concrete declarations) are always in classes. This means we _always_
 * can generate a bridge near an implementation (of course, in case it has a super-declaration with a different signature). Ultimately this
 * eases the process of determining what bridges are already generated in our supertypes and need to be inherited, not regenerated.
 */
open class DescriptorBasedFunctionHandle(val descriptor: FunctionDescriptor) : FunctionHandle {
    private val _overridden by lazy(LazyThreadSafetyMode.NONE) {
        descriptor.overriddenDescriptors.map {
            createHandleForOverridden(
                it.original
            )
        }
    }

    protected open fun createHandleForOverridden(overridden: FunctionDescriptor) = DescriptorBasedFunctionHandle(overridden)

    override val isDeclaration: Boolean = descriptor.kind.isReal || findInterfaceImplementation(descriptor) != null

    override val isAbstract: Boolean =
        descriptor.modality == Modality.ABSTRACT

    override val mayBeUsedAsSuperImplementation: Boolean =
        !DescriptorUtils.isInterface(descriptor.containingDeclaration)

    override fun getOverridden() = _overridden

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is DescriptorBasedFunctionHandle && descriptor == other.descriptor
    }

    override fun toString(): String {
        return descriptor.toString()
    }
}