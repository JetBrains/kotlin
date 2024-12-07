/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.StandardClassIds

interface StdlibClassFinder {
    fun findEnumEntriesClass(moduleDescriptor: ModuleDescriptor): ClassDescriptor?
}

/**
 * Default implementation which works for CLI, because all dependencies are added to one module,
 * so we can always find `EnumEntries` there.
 * But this doesn't work for IDE for modules without stdlib in dependencies (e.g. JDK or Kotlin module with `-no-stdlib` specified).
 */
private object CliStdlibClassFinderImpl : StdlibClassFinder {
    override fun findEnumEntriesClass(moduleDescriptor: ModuleDescriptor): ClassDescriptor? {
        return moduleDescriptor.findClassAcrossModuleDependencies(StandardClassIds.EnumEntries)
    }
}

val STDLIB_CLASS_FINDER_CAPABILITY = ModuleCapability<StdlibClassFinder>("StdlibClassFinder")

internal fun ModuleDescriptor.getStdlibClassFinder(): StdlibClassFinder {
    return getCapability(STDLIB_CLASS_FINDER_CAPABILITY) ?: CliStdlibClassFinderImpl
}