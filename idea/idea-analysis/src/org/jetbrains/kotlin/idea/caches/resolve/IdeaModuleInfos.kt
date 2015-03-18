/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.name.Name
import java.util.LinkedHashSet
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.utils.emptyOrSingletonList

public val LIBRARY_NAME_PREFIX: String = "library "

public abstract class IdeaModuleInfo : ModuleInfo {
    abstract fun contentScope(): GlobalSearchScope
}

private fun orderEntryToModuleInfo(project: Project, orderEntry: OrderEntry, productionOnly: Boolean): List<IdeaModuleInfo> {
    fun Module.toInfos() = if (productionOnly) listOf(productionSourceInfo()) else listOf(testSourceInfo(), productionSourceInfo())

    return when (orderEntry) {
        is ModuleSourceOrderEntry -> {
            orderEntry.getOwnerModule().toInfos()
        }
        is ModuleOrderEntry -> {
            orderEntry.getModule()?.toInfos().orEmpty()
        }
        is LibraryOrderEntry -> {
            val library = orderEntry.getLibrary() ?: return listOf()
            emptyOrSingletonList(LibraryInfo(project, library))
        }
        is JdkOrderEntry -> {
            val sdk = orderEntry.getJdk() ?: return listOf()
            emptyOrSingletonList(SdkInfo(project, sdk))
        }
        else -> {
            throw IllegalStateException("Unexpected order entry $orderEntry")
        }
    }
}

fun ideaModelDependencies(module: Module, productionOnly: Boolean): List<IdeaModuleInfo> {
    //NOTE: lib dependencies can be processed several times during recursive traversal
    val result = LinkedHashSet<IdeaModuleInfo>()
    val dependencyEnumerator = ModuleRootManager.getInstance(module).orderEntries().compileOnly().recursively().exportedOnly()
    if (productionOnly) {
        dependencyEnumerator.productionOnly()
    }
    dependencyEnumerator.forEach {
        orderEntry ->
        result.addAll(orderEntryToModuleInfo(module.getProject(), orderEntry!!, productionOnly))
        true
    }
    return result.toList()
}

public abstract class ModuleSourceInfo : IdeaModuleInfo() {
    abstract val module: Module
}

public data class ModuleProductionSourceInfo(override val module: Module) : ModuleSourceInfo() {
    override val name = Name.special("<production sources for module ${module.getName()}>")

    override fun contentScope() = ModuleProductionSourceScope(module)

    override fun dependencies() = ideaModelDependencies(module, productionOnly = true)

    override fun friends() = listOf(module.testSourceInfo())
}

//TODO: (module refactoring) do not create ModuleTestSourceInfo when there are no test roots for module
public data class ModuleTestSourceInfo(override val module: Module) : ModuleSourceInfo() {
    override val name = Name.special("<test sources for module ${module.getName()}>")

    override fun contentScope() = ModuleTestSourceScope(module)

    override fun dependencies() = ideaModelDependencies(module, productionOnly = false)
}

private fun ModuleSourceInfo.isTests() = this is ModuleTestSourceInfo

public fun Module.productionSourceInfo(): ModuleProductionSourceInfo = ModuleProductionSourceInfo(this)
public fun Module.testSourceInfo(): ModuleTestSourceInfo = ModuleTestSourceInfo(this)

private abstract data class ModuleSourceScope(val module: Module) : GlobalSearchScope(module.getProject()) {
    override fun compare(file1: VirtualFile, file2: VirtualFile) = 0
    override fun isSearchInModuleContent(aModule: Module) = aModule == module
    override fun isSearchInLibraries() = false

    // KT-6206
    override fun hashCode(): Int = module.hashCode()
}

private class ModuleProductionSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex()

    override fun contains(file: VirtualFile) = moduleFileIndex.isInSourceContent(file) && !moduleFileIndex.isInTestSourceContent(file)
}

private class ModuleTestSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).getFileIndex()

    override fun contains(file: VirtualFile) = moduleFileIndex.isInTestSourceContent(file)
}

public data class LibraryInfo(val project: Project, val library: Library) : IdeaModuleInfo() {
    override val name: Name = Name.special("<$LIBRARY_NAME_PREFIX${library.getName()}>")

    override fun contentScope() = LibraryWithoutSourceScope(project, library)

    override val isLibrary: Boolean
        get() = true

    override fun dependencies(): List<IdeaModuleInfo> {
        val result = LinkedHashSet<IdeaModuleInfo>()
        result.add(this)

        val (libraries, sdks) = LibraryDependenciesCache(project).getLibrariesAndSdksUsedWith(library)

        sdks.mapTo(result) { SdkInfo(project, it) }
        libraries.mapTo(result) { LibraryInfo(project, it) }

        return result.toList()
    }

    override fun toString() = "LibraryInfo(libraryName=${library.getName()})"
}

private data class LibrarySourceInfo(val project: Project, val library: Library) : IdeaModuleInfo() {
    override val name: Name = Name.special("<sources for library ${library.getName()}>")

    override fun contentScope() = GlobalSearchScope.EMPTY_SCOPE

    override val isLibrary: Boolean
        get() = true

    override fun dependencies(): List<IdeaModuleInfo> {
        return listOf(this) + LibraryInfo(project, library).dependencies()
    }

    override fun toString() = "LibrarySourceInfo(libraryName=${library.getName()})"
}

//TODO: (module refactoring) there should be separate SdkSourceInfo but there are no kotlin source in existing sdks for now :)
public data class SdkInfo(val project: Project, val sdk: Sdk) : IdeaModuleInfo() {
    override val name: Name = Name.special("<$LIBRARY_NAME_PREFIX${sdk.getName()}>")

    override fun contentScope() = SdkScope(project, sdk)

    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
}

private object NotUnderContentRootModuleInfo : IdeaModuleInfo() {
    override val name: Name = Name.special("<special module for files not under source root>")

    override fun contentScope() = GlobalSearchScope.EMPTY_SCOPE

    //TODO: (module refactoring) dependency on runtime can be of use here
    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
}

private data class LibraryWithoutSourceScope(project: Project, private val library: Library) :
        LibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), array<VirtualFile>()) {
}

//TODO: (module refactoring) android sdk has modified scope
private data class SdkScope(project: Project, private val sdk: Sdk) :
        LibraryScopeBase(project, sdk.getRootProvider().getFiles(OrderRootType.CLASSES), array<VirtualFile>())

private fun IdeaModuleInfo.isLibraryClasses() = this is SdkInfo || this is LibraryInfo
