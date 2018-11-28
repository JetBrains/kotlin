/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.metadata.serialization.StringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.ErrorUtils

interface DescriptorAwareStringTable : StringTable {
    fun getQualifiedClassNameIndex(classId: ClassId): Int =
        getQualifiedClassNameIndex(classId.asString(), classId.isLocal)

    fun getFqNameIndex(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: ${renderDescriptor(descriptor)}")
        }

        val classId = descriptor.classId
            ?: getLocalClassIdReplacement(descriptor)
            ?: throw IllegalStateException("Cannot get FQ name of local class: ${renderDescriptor(descriptor)}")

        return getQualifiedClassNameIndex(classId)
    }

    fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? = null

    private fun renderDescriptor(descriptor: ClassifierDescriptorWithTypeParameters): String =
        DescriptorRenderer.COMPACT.render(descriptor) + " defined in " +
                DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor.containingDeclaration)
}
