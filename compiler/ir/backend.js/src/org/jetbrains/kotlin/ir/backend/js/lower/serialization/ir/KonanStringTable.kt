/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import java.io.OutputStream

class KonanStringTable : StringTableImpl() {

    fun getClassOrPackageFqNameIndex(descriptor: ClassOrPackageFragmentDescriptor): Int {
        when (descriptor) {
            is PackageFragmentDescriptor -> 
                return getPackageFqNameIndex(descriptor.fqName)
            
            is ClassDescriptor -> 
                return getFqNameIndex(descriptor as ClassifierDescriptorWithTypeParameters) 
            else -> error("Can not get fqNameIndex for $descriptor")
        }
    }

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
                superClass?.classId ?: ClassId.topLevel(descriptor.module.builtIns.any.fqNameSafe)
            }
        } else {
            super.getLocalClassIdReplacement(descriptor)
        }
    }

    fun serializeTo(output: OutputStream) {
        val (strings, qualifiedNames) = buildProto()
        strings.writeDelimitedTo(output)
        qualifiedNames.writeDelimitedTo(output)
    }
}
