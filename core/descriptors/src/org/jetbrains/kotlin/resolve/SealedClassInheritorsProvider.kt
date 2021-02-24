/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

abstract class SealedClassInheritorsProvider {
    /**
     * This method may be called by compiler only for classes/interfaces with sealed modality
     */
    abstract fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor>
}

object CliSealedClassInheritorsProvider : SealedClassInheritorsProvider() {
    // Note this is a generic and slow implementation which would work almost for any subclass of ClassDescriptor.
    // Please avoid using it in new code.
    // TODO: do something more clever instead at call sites of this function
    override fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor> {
        if (sealedClass.modality != Modality.SEALED) return emptyList()

        val result = linkedSetOf<ClassDescriptor>()

        fun collectSubclasses(scope: MemberScope, collectNested: Boolean) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)) {
                if (descriptor !is ClassDescriptor) continue

                if (DescriptorUtils.isDirectSubclass(descriptor, sealedClass)) {
                    result.add(descriptor)
                }

                if (collectNested) {
                    collectSubclasses(descriptor.unsubstitutedInnerClassesScope, collectNested)
                }
            }
        }

        val container = if (!allowSealedInheritorsInDifferentFilesOfSamePackage) {
            sealedClass.containingDeclaration
        } else {
            sealedClass.parents.firstOrNull { it is PackageFragmentDescriptor }
        }
        if (container is PackageFragmentDescriptor) {
            collectSubclasses(
                container.getMemberScope(),
                collectNested = allowSealedInheritorsInDifferentFilesOfSamePackage
            )
        }
        collectSubclasses(sealedClass.unsubstitutedInnerClassesScope, collectNested = true)
        return result
    }

}
