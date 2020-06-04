/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.project.implementedDescriptors
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.multiplatform.*

fun DeclarationDescriptor.expectedDescriptors(): List<DeclarationDescriptor> = when {
    this is MemberDescriptor && isExpect -> listOf(this)

    this is MemberDescriptor && isActual -> module.implementedDescriptors.flatMap { findAnyExpectForActual(it) }

    this is ValueParameterDescriptor -> containingDeclaration.expectedDescriptors().mapNotNull {
        (it as? CallableDescriptor)?.valueParameters?.getOrNull(index)
    }

    else -> emptyList()
}


fun KtDeclaration.expectedDeclarations(): List<KtDeclaration> =
    (toDescriptor() as? MemberDescriptor)?.expectedDescriptors()?.mapNotNull {
        DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
    }.orEmpty()

// TODO: Sort out the cases with multiple expected descriptors
fun DeclarationDescriptor.expectedDescriptor(): DeclarationDescriptor? = expectedDescriptors().firstOrNull()
fun KtDeclaration.expectedDeclaration(): KtDeclaration? = expectedDeclarations().firstOrNull()

fun ModuleDescriptor.hasActualsFor(descriptor: MemberDescriptor) =
    actualsFor(descriptor).isNotEmpty()

fun ModuleDescriptor.actualsFor(descriptor: MemberDescriptor): List<DeclarationDescriptor> =
    descriptor.findAnyActualForExpected(this@actualsFor).filter { (it as? MemberDescriptor)?.isEffectivelyActual() == true }

private fun MemberDescriptor.isEffectivelyActual(checkConstructor: Boolean = true): Boolean =
    isActual || isEnumEntryInActual() || isConstructorInActual(checkConstructor)

private fun MemberDescriptor.isConstructorInActual(checkConstructor: Boolean) =
    checkConstructor && this is ClassConstructorDescriptor && containingDeclaration.isEffectivelyActual(checkConstructor)

private fun MemberDescriptor.isEnumEntryInActual() =
    (DescriptorUtils.isEnumEntry(this) && (containingDeclaration as? MemberDescriptor)?.isActual == true)

fun DeclarationDescriptor.actualDescriptors(): Collection<DeclarationDescriptor> {
    if (this is MemberDescriptor) {
        if (!this.isExpect) return emptyList()

        return module.implementingDescriptors.flatMap { it.actualsFor(this) }
    }

    if (this is ValueParameterDescriptor) {
        return containingDeclaration.actualDescriptors().mapNotNull { (it as? CallableDescriptor)?.valueParameters?.getOrNull(index) }
    }

    return emptyList()
}

fun KtDeclaration.actualDeclarations(): Set<KtDeclaration> =
    resolveToDescriptorIfAny(BodyResolveMode.FULL)
        ?.actualDescriptors()
        ?.mapNotNullTo(LinkedHashSet()) {
            DescriptorToSourceUtils.descriptorToDeclaration(it) as? KtDeclaration
        } ?: emptySet()

fun KtDeclaration.isExpectDeclaration(): Boolean = if (hasExpectModifier())
    true
else
    containingClassOrObject?.isExpectDeclaration() == true

fun KtDeclaration.hasMatchingExpected() = (toDescriptor() as? MemberDescriptor)?.expectedDescriptor() != null

fun KtDeclaration.isEffectivelyActual(checkConstructor: Boolean = true): Boolean = when {
    hasActualModifier() -> true
    this is KtEnumEntry || checkConstructor && this is KtConstructor<*> -> containingClass()?.hasActualModifier() == true
    else -> false
}

fun KtDeclaration.runOnExpectAndAllActuals(checkExpect: Boolean = true, useOnSelf: Boolean = false, f: (KtDeclaration) -> Unit) {
    if (hasActualModifier()) {
        val expectElement = expectedDeclaration()
        expectElement?.actualDeclarations()?.forEach {
            if (it !== this) {
                f(it)
            }
        }
        expectElement?.let { f(it) }
    } else if (!checkExpect || isExpectDeclaration()) {
        actualDeclarations().forEach { f(it) }
    }

    if (useOnSelf) f(this)
}

fun KtDeclaration.collectAllExpectAndActualDeclaration(withSelf: Boolean = true): Set<KtDeclaration> = when {
    isExpectDeclaration() -> actualDeclarations()
    hasActualModifier() -> expectedDeclaration()?.let { it.actualDeclarations() + it - this }.orEmpty()
    else -> emptySet()
}.let { if (withSelf) it + this else it }

fun KtDeclaration.runCommandOnAllExpectAndActualDeclaration(
    command: String = "",
    writeAction: Boolean = false,
    withSelf: Boolean = true,
    f: (KtDeclaration) -> Unit
) {
    val (pointers, project) = runReadAction {
        collectAllExpectAndActualDeclaration(withSelf).map { it.createSmartPointer() } to project
    }

    fun process() {
        for (pointer in pointers) {
            val declaration = pointer.element ?: continue
            f(declaration)
        }
    }

    if (writeAction) {
        project.executeWriteCommand(command, ::process)
    } else {
        project.executeCommand(command, command = ::process)
    }
}