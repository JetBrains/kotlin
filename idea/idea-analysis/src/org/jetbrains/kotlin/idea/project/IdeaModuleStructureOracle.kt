/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.implementingDescriptors
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import java.util.*

class IdeaModuleStructureOracle : ModuleStructureOracle {
    override fun hasImplementingModules(module: ModuleDescriptor): Boolean {
        return module.implementingDescriptors.isNotEmpty()
    }

    override fun findAllActualizationPaths(module: ModuleDescriptor): List<ModulePath> {
        val currentPath: Stack<ModuleDescriptor> = Stack()

        return sequence<ModulePath> {
            yieldPathsFromSubgraph(module, currentPath, getChilds = { it.implementingDescriptors })
        }.toList()
    }

    override fun findAllExpectedByPaths(module: ModuleDescriptor): List<ModulePath> {
        val currentPath: Stack<ModuleDescriptor> = Stack()

        return sequence<ModulePath> {
            yieldPathsFromSubgraph(module, currentPath, getChilds = { it.expectedByModules })
        }.toList()
    }

    private suspend fun SequenceScope<ModulePath>.yieldPathsFromSubgraph(
        root: ModuleDescriptor,
        currentPath: Stack<ModuleDescriptor>,
        getChilds: (ModuleDescriptor) -> List<ModuleDescriptor>
    ) {
        currentPath.push(root)

        val childs = getChilds(root)
        if (childs.isEmpty()) {
            yield(ModulePath(currentPath.toList()))
        } else {
            childs.forEach {
                yieldPathsFromSubgraph(it, currentPath, getChilds)
            }
        }

        currentPath.pop()
    }
}