/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.ModuleOrderEntryImpl
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.analyzer.CombinedModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.TrackableModuleInfo
import org.jetbrains.kotlin.caches.project.LibraryModuleInfo
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.framework.getLibraryPlatform
import org.jetbrains.kotlin.idea.project.KotlinModuleModificationTracker
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.GlobalSearchScopeWithModuleSources
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

internal val LOG = Logger.getInstance(IdeaModuleInfo::class.java)

@Suppress("DEPRECATION_ERROR")
interface IdeaModuleInfo : org.jetbrains.kotlin.idea.caches.resolve.IdeaModuleInfo {
    fun contentScope(): GlobalSearchScope

    val moduleOrigin: ModuleOrigin

    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = super.capabilities + mapOf(OriginCapability to moduleOrigin)

    override fun dependencies(): List<IdeaModuleInfo>
}

private fun orderEntryToModuleInfo(project: Project, orderEntry: OrderEntry, forProduction: Boolean): List<IdeaModuleInfo> {
    fun Module.toInfos() = correspondingModuleInfos().filter { !forProduction || it is ModuleProductionSourceInfo }

    if (!orderEntry.isValid) return emptyList()

    return when (orderEntry) {
        is ModuleSourceOrderEntry -> {
            orderEntry.getOwnerModule().toInfos()
        }
        is ModuleOrderEntry -> {
            val module = orderEntry.module ?: return emptyList()
            if (forProduction && orderEntry is ModuleOrderEntryImpl && orderEntry.isProductionOnTestDependency) {
                listOfNotNull(module.testSourceInfo())
            } else {
                module.toInfos()
            }
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

private fun OrderEntry.acceptAsDependency(forProduction: Boolean): Boolean {
    return this !is ExportableOrderEntry
            || !forProduction
            // this is needed for Maven/Gradle projects with "production-on-test" dependency
            || this is ModuleOrderEntryImpl && isProductionOnTestDependency
            || scope.isForProductionCompile
}

private fun ideaModelDependencies(
    module: Module,
    forProduction: Boolean,
    platform: TargetPlatform
): List<IdeaModuleInfo> {
    //NOTE: lib dependencies can be processed several times during recursive traversal
    val result = LinkedHashSet<IdeaModuleInfo>()
    val dependencyEnumerator = ModuleRootManager.getInstance(module).orderEntries().compileOnly().recursively().exportedOnly()
    if (forProduction && module.getBuildSystemType() == BuildSystemType.JPS) {
        dependencyEnumerator.productionOnly()
    }
    dependencyEnumerator.forEach { orderEntry ->
        if (orderEntry.acceptAsDependency(forProduction)) {
            result.addAll(orderEntryToModuleInfo(module.project, orderEntry!!, forProduction))
        }
        true
    }
    return result.filterNot { it is LibraryInfo && it.platform != platform }
}

interface ModuleSourceInfo : IdeaModuleInfo, TrackableModuleInfo {
    val module: Module

    override val expectedBy: List<ModuleSourceInfo>

    override val displayedName get() = module.name

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    override val platform: TargetPlatform
        get() = TargetPlatformDetector.getPlatform(module)

    override fun createModificationTracker(): ModificationTracker =
        KotlinModuleModificationTracker(module)
}

sealed class ModuleSourceInfoWithExpectedBy(private val forProduction: Boolean) : ModuleSourceInfo {
    override val expectedBy: List<ModuleSourceInfo>
        get() {
            val expectedByModules = module.findImplementedModules()
            return expectedByModules.mapNotNull { if (forProduction) it.productionSourceInfo() else it.testSourceInfo() }
        }

    override fun dependencies(): List<IdeaModuleInfo> = module.cached(createCachedValueProvider {
        CachedValueProvider.Result(
            ideaModelDependencies(module, forProduction, platform),
            ProjectRootModificationTracker.getInstance(module.project)
        )
    })

    // NB: CachedValueProvider must exist separately in Production / Test source info,
    // otherwise caching does not work properly
    protected abstract fun <T> createCachedValueProvider(f: () -> CachedValueProvider.Result<T>): CachedValueProvider<T>
}

data class ModuleProductionSourceInfo internal constructor(
    override val module: Module
) : ModuleSourceInfoWithExpectedBy(forProduction = true) {

    override val name = Name.special("<production sources for module ${module.name}>")

    override fun contentScope(): GlobalSearchScope = ModuleProductionSourceScope(module)

    override fun <T> createCachedValueProvider(f: () -> CachedValueProvider.Result<T>) = CachedValueProvider { f() }
}

//TODO: (module refactoring) do not create ModuleTestSourceInfo when there are no test roots for module
@Suppress("DEPRECATION_ERROR")
data class ModuleTestSourceInfo internal constructor(override val module: Module) :
    ModuleSourceInfoWithExpectedBy(forProduction = false), org.jetbrains.kotlin.idea.caches.resolve.ModuleTestSourceInfo {

    override val name = Name.special("<test sources for module ${module.name}>")

    override val displayedName get() = module.name + " (test)"

    override fun contentScope(): GlobalSearchScope = ModuleTestSourceScope(module)

    override fun modulesWhoseInternalsAreVisible() = module.cached(CachedValueProvider {
        val list = SmartList<ModuleInfo>()

        list.addIfNotNull(module.productionSourceInfo())

        TestModuleProperties.getInstance(module).productionModule?.let {
            list.addIfNotNull(it.productionSourceInfo())
        }

        CachedValueProvider.Result(list, ProjectRootModificationTracker.getInstance(module.project))
    })

    override fun <T> createCachedValueProvider(f: () -> CachedValueProvider.Result<T>) = CachedValueProvider { f() }
}

internal fun ModuleSourceInfo.isTests() = this is ModuleTestSourceInfo

fun Module.productionSourceInfo(): ModuleProductionSourceInfo? = if (hasProductionRoots()) ModuleProductionSourceInfo(this) else null

fun Module.testSourceInfo(): ModuleTestSourceInfo? = if (hasTestRoots()) ModuleTestSourceInfo(this) else null

internal fun Module.correspondingModuleInfos(): List<ModuleSourceInfo> = listOf(testSourceInfo(), productionSourceInfo()).filterNotNull()

private fun Module.hasProductionRoots() = hasRootsOfType(JavaSourceRootType.SOURCE) || hasRootsOfType(KotlinSourceRootType.Source)
private fun Module.hasTestRoots() = hasRootsOfType(JavaSourceRootType.TEST_SOURCE) || hasRootsOfType(KotlinSourceRootType.TestSource)

private fun Module.hasRootsOfType(sourceRootType: JpsModuleSourceRootType<*>): Boolean =
    rootManager.contentEntries.any { it.getSourceFolders(sourceRootType).isNotEmpty() }

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

    override fun contains(file: VirtualFile) =
        moduleFileIndex.isInSourceContentWithoutInjected(file) && !moduleFileIndex.isInTestSourceContentKotlinAware(file)

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

    override fun contains(file: VirtualFile) = moduleFileIndex.isInTestSourceContentKotlinAware(file)

    override fun toString() = "ModuleTestSourceScope($module)"
}

class LibraryInfo(val project: Project, val library: Library) : IdeaModuleInfo, LibraryModuleInfo, BinaryModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val name: Name = Name.special("<library ${library.name}>")

    override fun contentScope(): GlobalSearchScope = LibraryWithoutSourceScope(project, library)

    override fun dependencies(): List<IdeaModuleInfo> {
        val result = LinkedHashSet<IdeaModuleInfo>()
        result.add(this)

        val (libraries, sdks) = LibraryDependenciesCache.getInstance(project).getLibrariesAndSdksUsedWith(library)

        sdks.mapTo(result) { SdkInfo(project, it) }
        libraries.filter { it is LibraryEx && !it.isDisposed }.mapTo(result) {
            LibraryInfo(
                project,
                it
            )
        }

        return result.toList()
    }

    override val platform: TargetPlatform
        get() = getLibraryPlatform(project, library)

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

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(
        LibrarySourceScope(
            project,
            library
        ), project)

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

object NotUnderContentRootModuleInfo : IdeaModuleInfo {
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
private class SdkScope(project: Project, val sdk: Sdk) :
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

class PlatformModuleInfo(
    internal val platformModule: ModuleSourceInfo,
    private val commonModules: List<ModuleSourceInfo>
) : IdeaModuleInfo, CombinedModuleInfo, TrackableModuleInfo {
    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = platformModule.capabilities

    override fun contentScope() = GlobalSearchScope.union(containedModules.map { it.contentScope() }.toTypedArray())

    override val containedModules: List<ModuleSourceInfo> = listOf(platformModule) + commonModules

    override val platform: TargetPlatform?
        get() = platformModule.platform

    override val moduleOrigin: ModuleOrigin
        get() = platformModule.moduleOrigin

    override fun dependencies() = platformModule.dependencies()

    override fun modulesWhoseInternalsAreVisible() = containedModules.flatMap { it.modulesWhoseInternalsAreVisible() }

    override val name: Name
        get() = Name.special("<Platform module ${platformModule.displayedName} including ${commonModules.map { it.displayedName }}>")

    override fun createModificationTracker() = platformModule.createModificationTracker()
}

fun IdeaModuleInfo.projectSourceModules(): List<ModuleSourceInfo>? =
    (this as? ModuleSourceInfo)?.let(::listOf) ?: (this as? PlatformModuleInfo)?.containedModules