/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.sourceType
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import java.util.*

class IdeaModuleStructureOracle : ModuleStructureOracle {
    override fun hasImplementingModules(module: ModuleDescriptor): Boolean {
        return module.implementingDescriptors.isNotEmpty()
    }

    override fun findAllReversedDependsOnPaths(module: ModuleDescriptor): List<ModulePath> {
        val currentPath: Stack<ModuleInfo> = Stack()

        return sequence<ModuleInfoPath> {
            val root = module.moduleInfo
            if (root != null) {
                yieldPathsFromSubgraph(
                    root,
                    currentPath,
                    getChilds = {
                        with(DependsOnGraphHelper) { it.unwrapModuleSourceInfo()?.predecessorsInDependsOnGraph() ?: emptyList() }
                    }
                )
            }
        }.map {
            it.toModulePath()
        }.toList()
    }

    override fun findAllDependsOnPaths(module: ModuleDescriptor): List<ModulePath> {
        val currentPath: Stack<ModuleInfo> = Stack()

        return sequence<ModuleInfoPath> {
            val root = module.moduleInfo
            if (root != null) {
                yieldPathsFromSubgraph(
                    root,
                    currentPath,
                    getChilds = {
                        with(DependsOnGraphHelper) { it.unwrapModuleSourceInfo()?.successorsInDependsOnGraph() ?: emptyList() }
                    }
                )
            }
        }.map {
            it.toModulePath()
        }.toList()
    }

    private suspend fun SequenceScope<ModuleInfoPath>.yieldPathsFromSubgraph(
        root: ModuleInfo,
        currentPath: Stack<ModuleInfo>,
        getChilds: (ModuleInfo) -> List<ModuleInfo>
    ) {
        currentPath.push(root)

        val childs = getChilds(root)
        if (childs.isEmpty()) {
            yield(ModuleInfoPath(currentPath.toList()))
        } else {
            childs.forEach {
                yieldPathsFromSubgraph(it, currentPath, getChilds)
            }
        }

        currentPath.pop()
    }

    private class ModuleInfoPath(val nodes: List<ModuleInfo>)

    private fun ModuleInfoPath.toModulePath(): ModulePath =
        ModulePath(nodes.mapNotNull { it.unwrapModuleSourceInfo()?.toDescriptor() })
}

object DependsOnGraphHelper {
    fun ModuleDescriptor.predecessorsInDependsOnGraph(): List<ModuleDescriptor> {
        return moduleSourceInfo
            ?.predecessorsInDependsOnGraph()
            ?.mapNotNull { it.toDescriptor() }
            ?: emptyList()
    }

    fun ModuleSourceInfo.predecessorsInDependsOnGraph(): List<ModuleSourceInfo> {
        return this.module.predecessorsInDependsOnGraph().mapNotNull { it.toInfo(sourceType) }
    }

    fun Module.predecessorsInDependsOnGraph(): List<Module> {
        return implementingModules
    }

    fun ModuleDescriptor.successorsInDependsOnGraph(): List<ModuleDescriptor> {
        return moduleSourceInfo
            ?.successorsInDependsOnGraph()
            ?.mapNotNull { it.toDescriptor() }
            ?: emptyList()
    }

    fun ModuleSourceInfo.successorsInDependsOnGraph(): List<ModuleSourceInfo> {
        return module.successorsInDependsOnGraph().mapNotNull { it.toInfo(sourceType) }
    }

    fun Module.successorsInDependsOnGraph(): List<Module> {
        return implementedModules
    }
}

private val ModuleDescriptor.moduleInfo: ModuleInfo?
    get() = getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()

private val ModuleDescriptor.moduleSourceInfo: ModuleSourceInfo?
    get() = moduleInfo as? ModuleSourceInfo