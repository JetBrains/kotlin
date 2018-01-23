/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ClassOrPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf

fun getJvmModuleNameForDeserializedDescriptor(descriptor: DeclarationDescriptor): String? {
    val parent = DescriptorUtils.getParentOfType(descriptor, ClassOrPackageFragmentDescriptor::class.java, false)

    when {
        parent is DeserializedClassDescriptor -> {
            val classProto = parent.classProto
            val nameResolver = parent.c.nameResolver
            return classProto.getExtension(JvmProtoBuf.classModuleName)
                ?.takeIf { classProto.hasExtension(JvmProtoBuf.classModuleName) }
                ?.let(nameResolver::getString)
                    ?: JvmAbi.DEFAULT_MODULE_NAME
        }
        descriptor is DeserializedMemberDescriptor -> {
            val source = descriptor.containerSource
            if (source is JvmPackagePartSource) {
                val packageProto = source.packageProto
                val nameResolver = source.nameResolver
                return packageProto.getExtension(JvmProtoBuf.packageModuleName)
                    ?.takeIf { packageProto.hasExtension(JvmProtoBuf.packageModuleName) }
                    ?.let(nameResolver::getString)
                        ?: JvmAbi.DEFAULT_MODULE_NAME
            }
        }
    }

    return null
}
