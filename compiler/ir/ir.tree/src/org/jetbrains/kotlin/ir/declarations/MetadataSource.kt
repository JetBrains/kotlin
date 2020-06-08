/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.Name

interface MetadataSource {
    val name: Name?

    abstract class DescriptorBased<D : DeclarationDescriptor> internal constructor(val descriptor: D) : MetadataSource {
        override val name: Name
            get() = descriptor.name
    }

    open class Class(descriptor: ClassDescriptor) : DescriptorBased<ClassDescriptor>(descriptor)

    open class File(val descriptors: List<DeclarationDescriptor>) : MetadataSource {
        override val name: Name?
            get() = null
    }

    open class Function(descriptor: FunctionDescriptor) : DescriptorBased<FunctionDescriptor>(descriptor)

    open class Property(descriptor: PropertyDescriptor) : DescriptorBased<PropertyDescriptor>(descriptor)
}
