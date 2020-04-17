/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.sourceType
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.CompositePackageMemberScopeProvider
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
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
                    getChildren = {
                        it.unwrapModuleSourceInfo()?.predecessorsInDependsOnGraph() ?: emptyList()
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
            val root = module.moduleInfo?.unwrapModuleSourceInfo()
            if (root != null) {
                yieldPathsFromSubgraph(
                    root,
                    currentPath,
                    getChildren = {
                        it.unwrapModuleSourceInfo()?.successorsInDependsOnGraph() ?: emptyList()
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
        getChildren: (ModuleInfo) -> List<ModuleInfo>
    ) {
        currentPath.push(root)

        val childs = getChildren(root)
        if (childs.isEmpty()) {
            yield(ModuleInfoPath(currentPath.toList()))
        } else {
            childs.forEach {
                yieldPathsFromSubgraph(it, currentPath, getChildren)
            }
        }

        currentPath.pop()
    }

    private class ModuleInfoPath(val nodes: List<ModuleInfo>)

    private fun ModuleInfoPath.toModulePath(): ModulePath {
        val scopesInPath = nodes.mapNotNull {
            if (it is PackOfAndroidModules)
                it.toProvider()
            else
                it.unwrapModuleSourceInfo()?.toDescriptor()
        }
        return ModulePath(scopesInPath)
    }
}

private val ModuleDescriptor.moduleInfo: ModuleInfo?
    get() = getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()

private fun ModuleSourceInfo.predecessorsInDependsOnGraph(): List<ModuleSourceInfo> {
    return this.module.implementingModules.mapNotNull { it.toInfo(sourceType) }
}

private fun ModuleSourceInfo.successorsInDependsOnGraph(): List<ModuleSourceInfo> {
    return module.implementedModules.mapNotNull { it.toInfo(sourceType) }
}

private class PackOfAndroidModules(val androidInfos: List<ModuleInfo>) : ModuleInfo {
    fun toProvider(): CompositePackageMemberScopeProvider? {
        val individualProviders = androidInfos.mapNotNull { it.unwrapModuleSourceInfo()?.toDescriptor() }
        return if (individualProviders.isEmpty()) null else CompositePackageMemberScopeProvider(individualProviders)
    }

    override val name: Name
        get() = error("Should not be called")

    override fun dependencies(): List<ModuleInfo> {
        error("Should not be called")
    }

    override val platform: TargetPlatform
        get() = error("Should not be called")

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = error("Should not be called")
}