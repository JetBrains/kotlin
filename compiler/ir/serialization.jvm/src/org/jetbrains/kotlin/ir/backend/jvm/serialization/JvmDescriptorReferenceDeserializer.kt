/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.DescriptorReferenceDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DescriptorUniqIdAware
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName

class JvmDescriptorReferenceDeserializer(currentModule: ModuleDescriptor, private val uniqIdAware: DescriptorUniqIdAware) :
    DescriptorReferenceDeserializer(currentModule, JvmMangler, builtIns = null, resolvedForwardDeclarations = mutableMapOf()),
    DescriptorUniqIdAware by uniqIdAware {

    override fun resolveSpecialDescriptor(fqn: FqName): ClassDescriptor {
        error("Should never be called")
    }

    override fun checkIfSpecialDescriptorId(id: Long) = false

    override fun getDescriptorIdOrNull(descriptor: DeclarationDescriptor): Long? = null
}