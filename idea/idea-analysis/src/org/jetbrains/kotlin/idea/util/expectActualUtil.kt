/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun MemberDescriptor.expectedDescriptors() = module.implementedDescriptors.mapNotNull { it.declarationOf(this) }

// TODO: Sort out the cases with multiple expected descriptors
fun MemberDescriptor.expectedDescriptor() = expectedDescriptors().firstOrNull()

fun KtDeclaration.expectedDeclarationIfAny(): KtDeclaration? {
    val expectedDescriptor = (toDescriptor() as? MemberDescriptor)?.expectedDescriptor() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(expectedDescriptor) as? KtDeclaration
}

fun DeclarationDescriptor.liftToExpected(): DeclarationDescriptor? {
    if (this is MemberDescriptor) {
        return when {
            isExpect -> this
            isActual -> expectedDescriptor()
            else -> null
        }
    }

    if (this is ValueParameterDescriptor) {
        val containingExpectedDescriptor = containingDeclaration.liftToExpected() as? CallableDescriptor ?: return null
        return containingExpectedDescriptor.valueParameters.getOrNull(index)
    }

    return null
}

fun KtDeclaration.liftToExpected(): KtDeclaration? {
    val descriptor = resolveToDescriptorIfAny()
    val expectedDescriptor = descriptor?.liftToExpected() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(expectedDescriptor) as? KtDeclaration
}

fun KtParameter.liftToExpected(): KtParameter? {
    val parameterDescriptor = resolveToParameterDescriptorIfAny()
    val expectedDescriptor = parameterDescriptor?.liftToExpected() ?: return null
    return DescriptorToSourceUtils.descriptorToDeclaration(expectedDescriptor) as? KtParameter
}

fun ModuleDescriptor.hasDeclarationOf(descriptor: MemberDescriptor) = declarationOf(descriptor) != null

private fun ModuleDescriptor.declarationOf(descriptor: MemberDescriptor): DeclarationDescriptor? =
    with(ExpectedActualResolver) {
        val expectedCompatibilityMap = findExpectedForActual(descriptor, this@declarationOf)
        expectedCompatibilityMap?.get(ExpectedActualResolver.Compatibility.Compatible)?.firstOrNull()
            ?: expectedCompatibilityMap?.values?.flatten()?.firstOrNull()
    }

fun ModuleDescriptor.hasActualsFor(descriptor: MemberDescriptor) =
    actualsFor(descriptor).isNotEmpty()

fun ModuleDescriptor.actualsFor(descriptor: MemberDescriptor, checkCompatible: Boolean = false): List<DeclarationDescriptor> =
    with(ExpectedActualResolver) {
        if (checkCompatible) {
            descriptor.findCompatibleActualForExpected(this@actualsFor)
        } else {
            descriptor.findAnyActualForExpected(this@actualsFor)
        }
    }.filter { (it as? MemberDescriptor)?.isEffectivelyActual() == true }

private fun DeclarationDescriptor.actualsForExpected(): Collection<DeclarationDescriptor> {
    if (this is MemberDescriptor) {
        if (!this.isExpect) return emptyList()

        return module.implementingDescriptors.flatMap { it.actualsFor(this) }
    }

    if (this is ValueParameterDescriptor) {
        return containingDeclaration.actualsForExpected().mapNotNull { (it as? CallableDescriptor)?.valueParameters?.getOrNull(index) }
    }

    return emptyList()
}

// null means "any platform" here
fun KtDeclaration.actualsForExpected(module: Module? = null): Set<KtDeclaration> =
    resolveToDescriptorIfAny(BodyResolveMode.FULL)
        ?.actualsForExpected()
        ?.filter { module == null || (it.module.getCapability(ModuleInfo.Capability) as? ModuleSourceInfo)?.module == module }
        ?.mapNotNullTo(LinkedHashSet()) {
            DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
        } ?: emptySet()


fun KtDeclaration.isExpectDeclaration(): Boolean {
    if (hasExpectModifier()) return true
    if (this is KtClassOrObject) return this.isExpected()

    return containingClassOrObject?.isExpected() == true
}

private fun KtClassOrObject.isExpected(): Boolean {
    return this.hasExpectModifier() || this.descriptor.safeAs<ClassDescriptor>()?.isExpect == true
}

fun KtDeclaration.hasMatchingExpected() = (toDescriptor() as? MemberDescriptor)?.expectedDescriptor() != null

fun KtDeclaration.isEffectivelyActual(): Boolean {
    if (hasActualModifier()) return true

    val descriptor = toDescriptor() as? MemberDescriptor ?: return false
    return descriptor.isEffectivelyActual()
}

private fun MemberDescriptor.isEffectivelyActual(): Boolean =
    isActual || isEnumEntryInActual() || (this is ClassConstructorDescriptor && containingDeclaration.isEffectivelyActual())

private fun MemberDescriptor.isEnumEntryInActual() =
    (DescriptorUtils.isEnumEntry(this) && (containingDeclaration as? MemberDescriptor)?.isActual == true)

fun KtDeclaration.runOnExpectAndAllActuals(checkExpect: Boolean = true, f: (KtDeclaration) -> Unit) {
    if (hasActualModifier()) {
        val expectElement = liftToExpected()
        expectElement?.actualsForExpected()?.forEach {
            if (it !== this) {
                f(it)
            }
        }
        expectElement?.let { f(it) }
    } else if (!checkExpect || isExpectDeclaration()) {
        actualsForExpected().forEach { f(it) }
    }
}