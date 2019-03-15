/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

interface MetadataSource {
    class Class(val descriptor: ClassDescriptor) : MetadataSource

    class File(val descriptors: List<DeclarationDescriptor>) : MetadataSource

    class Function(val descriptor: FunctionDescriptor) : MetadataSource

    class Property(val descriptor: PropertyDescriptor) : MetadataSource
}
