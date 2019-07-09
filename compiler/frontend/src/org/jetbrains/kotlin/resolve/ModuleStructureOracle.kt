/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

interface ModuleStructureOracle {
    // May be faster than `findAllImplementingModules(module).isNotEmpty()`
    fun hasImplementingModules(module: ModuleDescriptor): Boolean

    fun findAllReversedDependsOnPaths(module: ModuleDescriptor): List<ModulePath>

    fun findAllDependsOnPaths(module: ModuleDescriptor): List<ModulePath>

    /**
     * Works like all sources are effectively in one module.
     *
     * This is the mode CLI currently operates in.
     */
    object SingleModule : ModuleStructureOracle {
        override fun hasImplementingModules(module: ModuleDescriptor): Boolean = false

        override fun findAllReversedDependsOnPaths(module: ModuleDescriptor): List<ModulePath> = listOf(ModulePath(module))

        override fun findAllDependsOnPaths(module: ModuleDescriptor): List<ModulePath> = listOf(ModulePath(module))
    }
}

class ModulePath(val nodes: List<ModuleDescriptor>) {
    constructor(vararg nodes: ModuleDescriptor) : this(nodes.toList())
}