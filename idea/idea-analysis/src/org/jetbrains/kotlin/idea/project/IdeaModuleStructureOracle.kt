/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ModulePath
import org.jetbrains.kotlin.resolve.ModuleStructureOracle
import java.util.*

fun findVirtualFile(file: VirtualDirectoryImpl): VirtualFile? {
    for (child in file.children) {
        if (child.url.endsWith(".kt")) {
            return child
        }

        if (child is VirtualDirectoryImpl) {
            val result = findVirtualFile(child)
            if (result != null) {
                return result
            }
        }
    }

    return null
}

fun findModuleDescriptors(
    library: Library,
    platform: TargetPlatform,
    psiManager: PsiManager,
    cacheService: KotlinCacheService
): Map<Name, ModuleDescriptor> {
    val descriptors = hashMapOf<Name, ModuleDescriptor>()

    for (sources in listOf(*library.getFiles(OrderRootType.SOURCES))) {
        if (sources !is VirtualDirectoryImpl) continue

        val virtualFileForKt = findVirtualFile(sources) ?: continue
        val fileViewProvider = psiManager.findViewProvider(virtualFileForKt)
        val ktFile = fileViewProvider!!.getPsi(fileViewProvider.baseLanguage) as? KtFile ?: continue
        val facade = cacheService.getResolutionFacade(listOf(ktFile), platform)

        descriptors[facade.moduleDescriptor.name] = facade.moduleDescriptor
    }

    return descriptors
}

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

        if (module.platform != null && module.getCapability(ModuleInfo.Capability) is LibrarySourceInfo) {
            val librarySourceInfo = module.getCapability(ModuleInfo.Capability) as LibrarySourceInfo

            val cacheService = KotlinCacheService.getInstance(librarySourceInfo.project)
            val psiManager = PsiManager.getInstance(librarySourceInfo.project)

            val descriptors = hashMapOf<Name, ModuleDescriptor>()

            for ((name, descriptor) in findModuleDescriptors(librarySourceInfo.library, module.platform!!, psiManager, cacheService)) {
                descriptors[name] = descriptor
            }

            for (moduleInfo in getModuleInfosFromIdeaModel(librarySourceInfo.project).filter { it.displayedName.contains("stdlib") }) {
                if (moduleInfo is LibraryInfo) {
                    for ((name, descriptor) in findModuleDescriptors(moduleInfo.library, module.platform!!, psiManager, cacheService)) {
                        descriptors[name] = descriptor
                    }
                }
            }

            return listOf(ModulePath(descriptors.map { it.value }.toList()))
        }

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