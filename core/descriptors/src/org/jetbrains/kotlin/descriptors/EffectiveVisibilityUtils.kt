/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext

fun EffectiveVisibility.toDescriptorVisibility(): DescriptorVisibility = DescriptorVisibilities.toDescriptorVisibility(toVisibility())

fun DescriptorVisibility.effectiveVisibility(
    descriptor: DeclarationDescriptor,
    checkPublishedApi: Boolean = false
): EffectiveVisibility {
    return customEffectiveVisibility() ?: normalize().forVisibility(descriptor, checkPublishedApi)
}

private fun DescriptorVisibility.forVisibility(descriptor: DeclarationDescriptor, checkPublishedApi: Boolean = false): EffectiveVisibility =
    when (this) {
        DescriptorVisibilities.PRIVATE_TO_THIS, DescriptorVisibilities.INVISIBLE_FAKE -> EffectiveVisibility.PrivateInClass
        DescriptorVisibilities.PRIVATE -> if (descriptor is ClassDescriptor &&
            descriptor.containingDeclaration is PackageFragmentDescriptor
        ) EffectiveVisibility.PrivateInFile else EffectiveVisibility.PrivateInClass
        DescriptorVisibilities.PROTECTED -> EffectiveVisibility.Protected(
            (descriptor.containingDeclaration as? ClassDescriptor)?.defaultType?.constructor
        )
        DescriptorVisibilities.INTERNAL -> if (!checkPublishedApi ||
            !descriptor.isPublishedApi()
        ) EffectiveVisibility.Internal else EffectiveVisibility.Public
        DescriptorVisibilities.PUBLIC -> EffectiveVisibility.Public
        DescriptorVisibilities.LOCAL -> EffectiveVisibility.Local
        // NB: visibility must be already normalized here, so e.g. no JavaVisibilities are possible at this point
        else -> throw AssertionError("Visibility $name is not allowed in forVisibility")
    }

data class DescriptorWithRelation(val descriptor: ClassifierDescriptor, private val relation: RelationToType) {
    fun effectiveVisibility() =
        (descriptor as? ClassDescriptor)?.visibility?.effectiveVisibility(descriptor, false) ?: EffectiveVisibility.Public

    override fun toString() = "$relation ${descriptor.name}"
}

private fun ClassifierDescriptor.dependentDescriptors(ownRelation: RelationToType): Set<DescriptorWithRelation> =
    setOf(DescriptorWithRelation(this, ownRelation)) +
            ((this.containingDeclaration as? ClassifierDescriptor)?.dependentDescriptors(ownRelation.containerRelation()) ?: emptySet())

fun ClassDescriptor.effectiveVisibility(checkPublishedApi: Boolean = false) = effectiveVisibility(emptySet(), checkPublishedApi)

private fun ClassDescriptor.effectiveVisibility(classes: Set<ClassDescriptor>, checkPublishedApi: Boolean): EffectiveVisibility =
    if (this in classes) EffectiveVisibility.Public
    else with(this.containingDeclaration as? ClassDescriptor) {
        lowerBound(
            visibility.effectiveVisibility(this@effectiveVisibility, checkPublishedApi),
            this?.effectiveVisibility(classes + this@effectiveVisibility, checkPublishedApi) ?: EffectiveVisibility.Public
        )
    }

// Should collect all dependent classifier descriptors, to get verbose diagnostic
private fun KotlinType.dependentDescriptors() = dependentDescriptors(emptySet(), RelationToType.CONSTRUCTOR)

private fun KotlinType.dependentDescriptors(types: Set<KotlinType>, ownRelation: RelationToType): Set<DescriptorWithRelation> {
    if (this in types) return emptySet()
    val ownDependent = constructor.declarationDescriptor?.dependentDescriptors(ownRelation) ?: emptySet()
    val argumentDependent = arguments.map { it.type.dependentDescriptors(types + this, RelationToType.ARGUMENT) }.flatten()
    return ownDependent + argumentDependent
}

private fun Set<DescriptorWithRelation>.leastPermissive(base: EffectiveVisibility): DescriptorWithRelation? {
    for (descriptorWithRelation in this) {
        val currentVisibility = descriptorWithRelation.effectiveVisibility()

        // Keep old behavior in K1 after KT-58988.
        if (currentVisibility is EffectiveVisibility.InternalOrPackage && base is EffectiveVisibility.InternalOrPackage) continue

        when (currentVisibility.relation(base, SimpleClassicTypeSystemContext)) {
            EffectiveVisibility.Permissiveness.LESS, EffectiveVisibility.Permissiveness.UNKNOWN -> {
                return descriptorWithRelation
            }
            else -> {
            }
        }
    }
    return null
}

fun KotlinType.leastPermissiveDescriptor(base: EffectiveVisibility) = dependentDescriptors().leastPermissive(base)

fun DeclarationDescriptorWithVisibility.effectiveVisibility(
    visibility: DescriptorVisibility = this.visibility,
    checkPublishedApi: Boolean = false
): EffectiveVisibility =
    lowerBound(
        visibility.effectiveVisibility(this, checkPublishedApi),
        (this.containingDeclaration as? ClassDescriptor)?.effectiveVisibility(checkPublishedApi) ?: EffectiveVisibility.Public
    )

private fun lowerBound(first: EffectiveVisibility, second: EffectiveVisibility): EffectiveVisibility {
    return first.lowerBound(second, SimpleClassicTypeSystemContext)
}
