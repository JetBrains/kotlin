/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name

interface MetadataSource {
    val name: Name?

    interface File : MetadataSource
    interface Class : MetadataSource
    interface Function : MetadataSource
    interface Property : MetadataSource {
        val isConst: Boolean
    }
}

sealed class DescriptorMetadataSource : MetadataSource {
    open val descriptor: Named?
        get() = null

    override val name: Name?
        get() = descriptor?.name

    class File(val descriptors: List<DeclarationDescriptor>) : DescriptorMetadataSource(), MetadataSource.File

    class Class(override val descriptor: ClassDescriptor) : DescriptorMetadataSource(), MetadataSource.Class

    class Function(override val descriptor: FunctionDescriptor) : DescriptorMetadataSource(), MetadataSource.Function

    class Property(override val descriptor: PropertyDescriptor) : DescriptorMetadataSource(), MetadataSource.Property {
        override val isConst: Boolean get() = descriptor.isConst
    }

    class LocalDelegatedProperty(override val descriptor: VariableDescriptorWithAccessors) : DescriptorMetadataSource(),
        MetadataSource.Property {
        override val isConst: Boolean get() = descriptor.isConst
    }
}
