/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

interface MetadataSource {
    open class Class(val descriptor: ClassDescriptor) : MetadataSource

    class File(val descriptors: List<DeclarationDescriptor>) : MetadataSource

    open class Function(val descriptor: FunctionDescriptor) : MetadataSource

    open class Property(val descriptor: PropertyDescriptor) : MetadataSource
}
