/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.typeUtil.supertypes

class MissingSupertypesResolver(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor
) {
    fun getMissingSuperClassifiers(descriptor: ClassifierDescriptor) = missingClassifiers(descriptor)

    private val missingClassifiers = storageManager.createMemoizedFunction { classifier: ClassifierDescriptor ->
        doGetMissingClassifiers(classifier)
    }

    private fun doGetMissingClassifiers(descriptor: ClassifierDescriptor): Set<ClassifierDescriptor> {
        val missingSuperClassifiers = mutableSetOf<ClassifierDescriptor>()
        val type = descriptor.defaultType

        for (supertype in type.supertypes()) {
            val supertypeDeclaration = supertype.constructor.declarationDescriptor

            /*
            * TODO: expects are not checked, because findClassAcrossModuleDependencies does not work with actualization via type alias
            * Type parameters are skipped here in favor to explicit checks for bounds, local declarations are ignored for optimization
            */
            if (supertypeDeclaration !is ClassDescriptor || supertypeDeclaration.isExpect) continue
            if (supertypeDeclaration.visibility == Visibilities.LOCAL) continue

            val superTypeClassId = supertypeDeclaration.classId ?: continue
            val dependency = moduleDescriptor.findClassAcrossModuleDependencies(superTypeClassId)

            if (dependency == null || dependency is NotFoundClasses.MockClassDescriptor) {
                missingSuperClassifiers.add(supertypeDeclaration)
            }
        }

        return missingSuperClassifiers.toSet()
    }
}
