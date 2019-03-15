/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.serialization.StringTableImpl

class JsKlibMetadataStringTable : StringTableImpl() {
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
                superClass?.classId ?: ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.any.toSafe())
            }
        } else {
            super.getLocalClassIdReplacement(descriptor)
        }
    }
}
