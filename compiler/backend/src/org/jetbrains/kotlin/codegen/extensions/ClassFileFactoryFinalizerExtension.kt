/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.extensions

import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor

/**
 * Extension point which is called after the compiler outputs have been finalized,
 * which allows inspecting all output files, except for jar manifests.
 */
interface ClassFileFactoryFinalizerExtension {
    companion object : ProjectExtensionDescriptor<ClassFileFactoryFinalizerExtension>(
        "org.jetbrains.kotlin.classFileFactoryFinalizerExtension", ClassFileFactoryFinalizerExtension::class.java)

    fun finalizeClassFactory(factory: ClassFileFactory)
}