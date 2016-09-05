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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized

fun <Signature> generateBridgesForFunctionDescriptor(
        descriptor: FunctionDescriptor,
        signature: (FunctionDescriptor) -> Signature,
        isBodyOwner: (DeclarationDescriptor) -> Boolean
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandle(descriptor, isBodyOwner), { signature(it.descriptor) })
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
class DescriptorBasedFunctionHandle(
        val descriptor: FunctionDescriptor,
        /*
        To generate proper bridges for non-abstract function
        we should know if the function declaration and its definition in the target platform are the same or not.
        For JS and JVM8 they are placed in interface classes and we need generate bridge for such function ('isAbstract' will return false).
        For JVM6 target function body generated in separate place (DefaultImpl) and method in interface is abstract
        For JVM6 target function body is generated in a separate place (DefaultImpls) and
        the method in the interface is abstract so we must not generate bridges for such cases.
        */
        isBodyOwner: (DeclarationDescriptor) -> Boolean
) : FunctionHandle {
    private val overridden = descriptor.overriddenDescriptors.map { DescriptorBasedFunctionHandle(it.original, isBodyOwner) }

    override val isDeclaration: Boolean =
            descriptor.kind.isReal ||
            findInterfaceImplementation(descriptor) != null

    override val isAbstract: Boolean =
            descriptor.modality == Modality.ABSTRACT ||
            isBodyOwner(descriptor.containingDeclaration)

    override val isInterfaceDeclaration: Boolean
        get() = DescriptorUtils.isInterface(descriptor.containingDeclaration)

    override fun getOverridden() = overridden

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


/**
 * Given a fake override in a class, returns an overridden declaration with implementation in trait, such that a method delegating to that
 * trait implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any trait implementation or such method was already generated into the superclass or is a method from Any.
 */
fun findInterfaceImplementation(descriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    if (descriptor.kind.isReal) return null
    if (isOrOverridesSynthesized(descriptor)) return null

    val implementation = findImplementationFromInterface(descriptor) ?: return null
    val immediateConcreteSuper = firstSuperMethodFromKotlin(descriptor, implementation) ?: return null

    if (!DescriptorUtils.isInterface(immediateConcreteSuper.containingDeclaration)) {
        // If this implementation is already generated into the superclass, we need not generate it again, it'll be inherited
        return null
    }

    return immediateConcreteSuper
}

/**
 * Given a fake override, returns an overridden non-abstract function from an interface which is the actual implementation of this function
 * that should be called when the given fake override is called.
 */
fun findImplementationFromInterface(descriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    val overridden = OverridingUtil.getOverriddenDeclarations(descriptor)
    val filtered = OverridingUtil.filterOutOverridden(overridden)

    val result = filtered.firstOrNull { it.modality != Modality.ABSTRACT } ?: return null

    if (DescriptorUtils.isClassOrEnumClass(result.containingDeclaration)) return null

    return result
}

/**
 * Given a fake override and its implementation (non-abstract declaration) somewhere in supertypes,
 * returns the first immediate super function of the given fake override which overrides that implementation.
 * The returned function should be called from TImpl-bridges generated for the given fake override.
 */
fun firstSuperMethodFromKotlin(
        descriptor: CallableMemberDescriptor,
        implementation: CallableMemberDescriptor
): CallableMemberDescriptor? {
    return descriptor.overriddenDescriptors.firstOrNull { overridden ->
        overridden.modality != Modality.ABSTRACT &&
        (overridden == implementation || OverridingUtil.overrides(overridden, implementation))
    }
}
