/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Type

abstract class KotlinTypeMapperBase {
    abstract fun mapClass(classifier: ClassifierDescriptor): Type

    fun mapDefaultImpls(descriptor: ClassDescriptor): Type =
        Type.getObjectType(mapClass(descriptor).internalName + JvmAbi.DEFAULT_IMPLS_SUFFIX)
}
