/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.TrackableModuleInfo
import org.jetbrains.kotlin.caches.resolve.LibraryModuleInfo
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.framework.getLibraryPlatform
import org.jetbrains.kotlin.idea.project.KotlinModuleModificationTracker
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.GlobalSearchScopeWithModuleSources
import java.util.*

interface IdeaModuleInfo : ModuleInfo {
    fun contentScope(): GlobalSearchScope

    val moduleOrigin: ModuleOrigin

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = super.capabilities + mapOf(OriginCapability to moduleOrigin)

    override fun dependencies(): List<IdeaModuleInfo>
}

private fun orderEntryToModuleInfo(project: Project, orderEntry: OrderEntry, productionOnly: Boolean): List<IdeaModuleInfo> {
    fun Module.toInfos() = if (productionOnly) listOf(productionSourceInfo()) else listOf(testSourceInfo(), productionSourceInfo())

    if (!orderEntry.isValid) return emptyList()

    return when (orderEntry) {
        is ModuleSourceOrderEntry -> {
            orderEntry.getOwnerModule().toInfos()
        }
        is ModuleOrderEntry -> {
            orderEntry.module?.toInfos().orEmpty()
        }
        is LibraryOrderEntry -> {
            val library = orderEntry.library ?: return listOf()
            listOfNotNull(LibraryInfo(project, library))
        }
        is JdkOrderEntry -> {
            val sdk = orderEntry.jdk ?: return listOf()
            listOfNotNull(SdkInfo(project, sdk))
        }
        else -> {
            throw IllegalStateException("Unexpected order entry $orderEntry")
        }
    }
}

private fun <T> Module.cached(provider: CachedValueProvider<T>): T {
    return CachedValuesManager.getManager(project).getCachedValue(this, provider)
}

private fun ideaModelDependencies(module: Module, productionOnly: Boolean): List<IdeaModuleInfo> {
    //NOTE: lib dependencies can be processed several times during recursive traversal
    val result = LinkedHashSet<IdeaModuleInfo>()
    val dependencyEnumerator = ModuleRootManager.getInstance(module).orderEntries().compileOnly().recursively().exportedOnly()
    if (productionOnly) {
        dependencyEnumerator.productionOnly()
    }
    dependencyEnumerator.forEach {
        orderEntry ->
        result.addAll(orderEntryToModuleInfo(module.project, orderEntry!!, productionOnly))
        true
    }
    return result.toList()
}

interface ModuleSourceInfo : IdeaModuleInfo, TrackableModuleInfo {
    val module: Module

    override val displayedName get() = module.name

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    override val platform: TargetPlatform
        get() = TargetPlatformDetector.getPlatform(module)

    override fun createModificationTracker(): ModificationTracker =
            KotlinModuleModificationTracker(module)
}

data class ModuleProductionSourceInfo(override val module: Module) : ModuleSourceInfo {
    override val name = Name.special("<production sources for module ${module.name}>")

    override fun contentScope(): GlobalSearchScope = ModuleProductionSourceScope(module)

    override fun dependencies() = module.cached(CachedValueProvider {
        CachedValueProvider.Result(
                ideaModelDependencies(module, productionOnly = true),
                ProjectRootModificationTracker.getInstance(module.project))
    })
}

//TODO: (module refactoring) do not create ModuleTestSourceInfo when there are no test roots for module
data class ModuleTestSourceInfo(override val module: Module) : ModuleSourceInfo {
    override val name = Name.special("<test sources for module ${module.name}>")

    override val displayedName get() = module.name + " (test)"

    override fun contentScope(): GlobalSearchScope = ModuleTestSourceScope(module)

    override fun dependencies() = module.cached(CachedValueProvider {
        CachedValueProvider.Result(
                ideaModelDependencies(module, productionOnly = false),
                ProjectRootModificationTracker.getInstance(module.project))
    })

    override fun modulesWhoseInternalsAreVisible() = module.cached(CachedValueProvider {
        val list = SmartList<ModuleInfo>(module.productionSourceInfo())

        TestModuleProperties.getInstance(module).productionModule?.let {
            list.add(it.productionSourceInfo())
        }

        CachedValueProvider.Result(list, ProjectRootModificationTracker.getInstance(module.project))
    })
}

internal fun ModuleSourceInfo.isTests() = this is ModuleTestSourceInfo

fun Module.productionSourceInfo(): ModuleProductionSourceInfo = ModuleProductionSourceInfo(this)
fun Module.testSourceInfo(): ModuleTestSourceInfo = ModuleTestSourceInfo(this)

private abstract class ModuleSourceScope(val module: Module) : GlobalSearchScope(module.project), GlobalSearchScopeWithModuleSources {
    override fun compare(file1: VirtualFile, file2: VirtualFile) = 0
    override fun isSearchInModuleContent(aModule: Module) = aModule == module
    override fun isSearchInLibraries() = false
}

private class ModuleProductionSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is ModuleProductionSourceScope && module == other.module)
    }
    // KT-6206
    override fun hashCode(): Int = 31 * module.hashCode()

    override fun contains(file: VirtualFile) = moduleFileIndex.isInSourceContentWithoutInjected(file) && !moduleFileIndex.isInTestSourceContent(file)

    override fun toString() = "ModuleProductionSourceScope($module)"
}

private class ModuleTestSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is ModuleTestSourceScope && module == other.module)
    }
    // KT-6206
    override fun hashCode(): Int = 37 * module.hashCode()

    override fun contains(file: VirtualFile) = moduleFileIndex.isInTestSourceContent(file)

    override fun toString() = "ModuleTestSourceScope($module)"
}

class LibraryInfo(val project: Project, val library: Library) : IdeaModuleInfo, LibraryModuleInfo, BinaryModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val name: Name = Name.special("<library ${library.name}>")

    override fun contentScope(): GlobalSearchScope = LibraryWithoutSourceScope(project, library)

    override val isLibrary: Boolean
        get() = true

    override fun dependencies(): List<IdeaModuleInfo> {
        val result = LinkedHashSet<IdeaModuleInfo>()
        result.add(this)

        val (libraries, sdks) = LibraryDependenciesCache.getInstance(project).getLibrariesAndSdksUsedWith(library)

        sdks.mapTo(result) { SdkInfo(project, it) }
        libraries.filter { it is LibraryEx && !it.isDisposed }.mapTo(result) { LibraryInfo(project, it) }

        return result.toList()
    }

    override val platform: TargetPlatform
        get() = getLibraryPlatform(library)

    override val sourcesModuleInfo: SourceForBinaryModuleInfo
        get() = LibrarySourceInfo(project, library)

    override fun getLibraryRoots(): Collection<String> =
            library.getFiles(OrderRootType.CLASSES).mapNotNull(PathUtil::getLocalPath)

    override fun toString() = "LibraryInfo(libraryName=${library.name})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is LibraryInfo && library == other.library)
    }

    override fun hashCode(): Int = 43 * library.hashCode()
}

data class LibrarySourceInfo(val project: Project, val library: Library) : IdeaModuleInfo, SourceForBinaryModuleInfo {

    override val name: Name = Name.special("<sources for library ${library.name}>")

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(LibrarySourceScope(project, library), project)

    override val isLibrary: Boolean
        get() = true

    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> {
        return listOf(LibraryInfo(project, library))
    }

    override val binariesModuleInfo: BinaryModuleInfo
        get() = LibraryInfo(project, library)

    override fun toString() = "LibrarySourceInfo(libraryName=${library.name})"
}

//TODO: (module refactoring) there should be separate SdkSourceInfo but there are no kotlin source in existing sdks for now :)
data class SdkInfo(val project: Project, val sdk: Sdk) : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val name: Name = Name.special("<sdk ${sdk.name}>")

    override fun contentScope(): GlobalSearchScope = SdkScope(project, sdk)

    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
}

internal object NotUnderContentRootModuleInfo : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<special module for files not under source root>")

    override fun contentScope() = GlobalSearchScope.EMPTY_SCOPE

    //TODO: (module refactoring) dependency on runtime can be of use here
    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)
}

private class LibraryWithoutSourceScope(project: Project, private val library: Library) :
        LibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), arrayOf<VirtualFile>()) {

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getClassRootForFile(file)

    override fun equals(other: Any?) = other is LibraryWithoutSourceScope && library == other.library

    override fun hashCode() = library.hashCode()

    override fun toString() = "LibraryWithoutSourceScope($library)"
}

private class LibrarySourceScope(project: Project, private val library: Library) :
        LibraryScopeBase(project, arrayOf<VirtualFile>(), library.getFiles(OrderRootType.SOURCES)) {

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getSourceRootForFile(file)

    override fun equals(other: Any?) = other is LibrarySourceScope && library == other.library

    override fun hashCode() = library.hashCode()

    override fun toString() = "LibrarySourceScope($library)"
}

//TODO: (module refactoring) android sdk has modified scope
private class SdkScope(project: Project, private val sdk: Sdk) :
        LibraryScopeBase(project, sdk.rootProvider.getFiles(OrderRootType.CLASSES), arrayOf<VirtualFile>()) {

    override fun equals(other: Any?) = other is SdkScope && sdk == other.sdk

    override fun hashCode() = sdk.hashCode()

    override fun toString() = "SdkScope($sdk)"
}

internal fun IdeaModuleInfo.isLibraryClasses() = this is SdkInfo || this is LibraryInfo

val OriginCapability = ModuleDescriptor.Capability<ModuleOrigin>("MODULE_ORIGIN")

enum class ModuleOrigin {
    MODULE,
    LIBRARY,
    OTHER
}

interface BinaryModuleInfo : IdeaModuleInfo {
    val sourcesModuleInfo: SourceForBinaryModuleInfo?
    fun binariesScope(): GlobalSearchScope {
        val contentScope = contentScope()
        return KotlinSourceFilterScope.libraryClassFiles(contentScope, contentScope.project!!)
    }
}

interface SourceForBinaryModuleInfo : IdeaModuleInfo {
    val binariesModuleInfo: BinaryModuleInfo
    fun sourceScope(): GlobalSearchScope

    // module infos for library source do not have contents in the following sense:
    // we can not provide a collection of files that is supposed to be analyzed in IDE independently
    //
    // as of now each source file is analyzed separately and depends on corresponding binaries
    // see KotlinCacheServiceImpl#createFacadeForSyntheticFiles
    override fun contentScope(): GlobalSearchScope = GlobalSearchScope.EMPTY_SCOPE

    override fun dependencies() = listOf(this) + binariesModuleInfo.dependencies()

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER
}