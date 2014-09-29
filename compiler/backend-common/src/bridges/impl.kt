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

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.OverrideResolver
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil
import org.jetbrains.jet.lang.types.TypeUtils

public fun <Signature> generateBridgesForFunctionDescriptor(
        descriptor: FunctionDescriptor,
        signature: (FunctionDescriptor) -> Signature
): Set<Bridge<Signature>> {
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
private data class DescriptorBasedFunctionHandle(val descriptor: FunctionDescriptor) : FunctionHandle {
    private val _overridden = descriptor.getOverriddenDescriptors().map { DescriptorBasedFunctionHandle(it.getOriginal()) }

    override val isDeclaration: Boolean =
            descriptor.getKind().isReal() ||
            findTraitImplementation(descriptor) != null

    override val isAbstract: Boolean =
            descriptor.getModality() == Modality.ABSTRACT ||
            DescriptorUtils.isTrait(descriptor.getContainingDeclaration())

    override fun getOverridden() = _overridden
}


/**
 * Given a fake override in a class, returns an overridden declaration with implementation in trait, such that a method delegating to that
 * trait implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any trait implementation or such method was already generated into some superclass
 */
public fun findTraitImplementation(descriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    if (descriptor.getKind().isReal()) return null
    if (CallResolverUtil.isOrOverridesSynthesized(descriptor)) return null

    // TODO: this logic is quite common for bridge generation, find a way to abstract it to a single place
    // TODO: don't use filterOutOverridden() here, it's an internal front-end utility (see its implementation)
    val overriddenDeclarations = OverrideResolver.getOverriddenDeclarations(descriptor)
    val filteredOverriddenDeclarations = OverrideResolver.filterOutOverridden(overriddenDeclarations)

    var implementation: CallableMemberDescriptor? = null
    for (overriddenDeclaration in filteredOverriddenDeclarations) {
        if (DescriptorUtils.isTrait(overriddenDeclaration.getContainingDeclaration()) && overriddenDeclaration.getModality() != Modality.ABSTRACT) {
            implementation = overriddenDeclaration
        }
    }
    if (implementation == null) {
        return null
    }

    // If this implementation is already generated into one of the superclasses, we need not generate it again, it'll be inherited
    val containingClass = descriptor.getContainingDeclaration() as ClassDescriptor
    val implClassType = implementation!!.getDispatchReceiverParameter()!!.getType()
    for (supertype in containingClass.getDefaultType().getConstructor().getSupertypes()) {
        if (!DescriptorUtils.isTrait(supertype.getConstructor().getDeclarationDescriptor()!!) &&
            TypeUtils.getAllSupertypes(supertype).contains(implClassType)) {
            return null
        }
    }

    return implementation
}
