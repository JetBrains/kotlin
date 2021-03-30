/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.cfg.containingDeclarationForPseudocode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.descriptorUtil.isTypeRefinementEnabled
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun KtElement.containingNonLocalDeclaration(): KtDeclaration? {
    var container = this.containingDeclarationForPseudocode
    while (container != null && KtPsiUtil.isLocal(container)) {
        container = container.containingDeclarationForPseudocode
    }
    return container
}

val KtDeclaration.isOrdinaryClass
    get() = this is KtClass &&
            !this.hasModifier(KtTokens.INLINE_KEYWORD) &&
            !this.isAnnotation() &&
            !this.isInterface()

val KtDeclaration.isAnnotated get() = this.annotationEntries.isNotEmpty()

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
 * Given a fake override in a class, returns an overridden declaration with implementation in trait, such that a method delegating to that
 * trait implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any trait implementation or such method was already generated into the superclass or is a method from Any.
 */
@JvmOverloads
fun findInterfaceImplementation(descriptor: CallableMemberDescriptor, returnImplNotDelegate: Boolean = false): CallableMemberDescriptor? {
    if (descriptor.kind.isReal) return null
    if (isOrOverridesSynthesized(descriptor)) return null

    val implementation = findImplementationFromInterface(descriptor) ?: return null
    val immediateConcreteSuper = firstSuperMethodFromKotlin(descriptor, implementation) ?: return null

    if (!DescriptorUtils.isInterface(immediateConcreteSuper.containingDeclaration)) {
        // If this implementation is already generated into the superclass, we need not generate it again, it'll be inherited
        return null
    }

    return if (returnImplNotDelegate) implementation else immediateConcreteSuper
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
                (overridden == implementation || OverridingUtil.overrides(
                    overridden,
                    implementation,
                    overridden.module.isTypeRefinementEnabled(),
                    true
                ))
    }
}


fun getNonPrivateTraitMembersForDelegation(
    descriptor: ClassDescriptor,
    returnImplNotDelegate: Boolean = false,
): Map<CallableMemberDescriptor, CallableMemberDescriptor> {
    val result = linkedMapOf<CallableMemberDescriptor, CallableMemberDescriptor>()
    for (declaration in DescriptorUtils.getAllDescriptors(descriptor.defaultType.memberScope)) {
        if (declaration !is CallableMemberDescriptor) continue
        result[declaration] = getNonPrivateTraitMembersForDelegation(declaration, returnImplNotDelegate) ?: continue
    }
    return result
}

fun getNonPrivateTraitMembersForDelegation(
    descriptor: CallableMemberDescriptor,
    returnImplNotDelegate: Boolean = false,
): CallableMemberDescriptor? {
    val traitMember = findInterfaceImplementation(descriptor, returnImplNotDelegate)
    if (traitMember == null ||
        DescriptorVisibilities.isPrivate(traitMember.visibility) ||
        traitMember.visibility == DescriptorVisibilities.INVISIBLE_FAKE
    ) return null
    return traitMember
}
