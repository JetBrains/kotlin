/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.serialization.StringTableImpl

class KlibMetadataStringTable : StringTableImpl() {
    override fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? {
        return if (descriptor.containingDeclaration is CallableMemberDescriptor) {
            val superClassifiers = descriptor.getAllSuperClassifiers()
                .mapNotNull { it as ClassifierDescriptorWithTypeParameters }
                .filter { it != descriptor }
                .toList()
            if (superClassifiers.size == 1) {
                superClassifiers[0].classId
            } else {
                val superClass = superClassifiers.find { !DescriptorUtils.isInterface(it) }
                superClass?.classId ?: ClassId.topLevel(StandardNames.FqNames.any.toSafe())
            }
        } else {
            super.getLocalClassIdReplacement(descriptor)
        }
    }
}
