/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.analyzer.CombinedModuleInfo
import org.jetbrains.kotlin.analyzer.LibraryModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.TrackableModuleInfo
import org.jetbrains.kotlin.caches.project.cacheByClassInvalidatingOnRootModifications
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.caches.resolve.resolution
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle
import org.jetbrains.kotlin.idea.caches.resolve.util.enlargedSearchScope
import org.jetbrains.kotlin.idea.caches.trackers.KotlinModuleOutOfCodeBlockModificationTracker
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import org.jetbrains.kotlin.idea.core.isInTestSourceContentKotlinAware
import org.jetbrains.kotlin.idea.framework.getLibraryPlatform
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.findAnalyzerServices
import org.jetbrains.kotlin.idea.project.getStableName
import org.jetbrains.kotlin.idea.project.isHMPPEnabled
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.isInSourceContentWithoutInjected
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.compat.toOldPlatform
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.types.typeUtil.closure
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

internal val LOG = Logger.getInstance(IdeaModuleInfo::class.java)

@Suppress("DEPRECATION_ERROR")
interface IdeaModuleInfo : org.jetbrains.kotlin.idea.caches.resolve.IdeaModuleInfo {
    fun contentScope(): GlobalSearchScope

    val moduleOrigin: ModuleOrigin

    val project: Project?

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
            createLibraryInfo(project, library)
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

val Project.libraryInfoCache: MutableMap<Library, List<LibraryInfo>>
    get() = cacheInvalidatingOnRootModifications { ContainerUtil.createConcurrentWeakMap() }

fun createLibraryInfo(project: Project, library: Library): List<LibraryInfo> {
    val cache = project.libraryInfoCache

    return cache.getOrPut(library) {
        getLibraryPlatform(project, library).idePlatformKind.resolution.createLibraryInfo(project, library)
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
    fun TargetPlatform.canDependOn(other: TargetPlatform): Boolean {
        return if (module.isHMPPEnabled) {
            other.componentPlatforms.containsAll(this.componentPlatforms)
        } else {
            this.isJvm() && other.isJvm() ||
                    this.isJs() && other.isJs() ||
                    this.isNative() && other.isNative() ||
                    this.isCommon() && other.isCommon()
        }
    }

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
    return result.filterNot { it is LibraryInfo && !platform.canDependOn(it.platform) }
}

interface ModuleSourceInfo : IdeaModuleInfo, TrackableModuleInfo {
    val module: Module

    override val expectedBy: List<ModuleSourceInfo>

    override val displayedName get() = module.name

    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.MODULE

    override val project: Project
        get() = module.project

    override val platform: TargetPlatform
        get() = TargetPlatformDetector.getPlatform(module)

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        message = "This accessor is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
        replaceWith = ReplaceWith("platform"),
        level = DeprecationLevel.ERROR
    )
    fun getPlatform(): org.jetbrains.kotlin.resolve.TargetPlatform = platform.toOldPlatform()

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = platform.findAnalyzerServices(module.project)

    override fun createModificationTracker(): ModificationTracker =
        KotlinModuleOutOfCodeBlockModificationTracker(module)
}

sealed class ModuleSourceInfoWithExpectedBy(private val forProduction: Boolean) : ModuleSourceInfo {
    override val expectedBy: List<ModuleSourceInfo>
        get() {
            val expectedByModules = module.implementedModules
            return expectedByModules.mapNotNull { if (forProduction) it.productionSourceInfo() else it.testSourceInfo() }
        }

    override fun dependencies(): List<IdeaModuleInfo> = module.cacheByClassInvalidatingOnRootModifications(this::class.java) {
        ideaModelDependencies(module, forProduction, platform)
    }
}

data class ModuleProductionSourceInfo internal constructor(
    override val module: Module
) : ModuleSourceInfoWithExpectedBy(forProduction = true) {

    override val name = Name.special("<production sources for module ${module.name}>")

    override val stableName: Name = module.getStableName()

    override fun contentScope(): GlobalSearchScope {
        return enlargedSearchScope(ModuleProductionSourceScope(module), module, isTestScope = false)
    }
}

//TODO: (module refactoring) do not create ModuleTestSourceInfo when there are no test roots for module
@Suppress("DEPRECATION_ERROR")
data class ModuleTestSourceInfo internal constructor(override val module: Module) :
    ModuleSourceInfoWithExpectedBy(forProduction = false), org.jetbrains.kotlin.idea.caches.resolve.ModuleTestSourceInfo {

    override val name = Name.special("<test sources for module ${module.name}>")

    override val displayedName get() = KotlinIdeaAnalysisBundle.message("module.name.0.test", module.name)

    override val stableName: Name = module.getStableName()

    override fun contentScope(): GlobalSearchScope = enlargedSearchScope(ModuleTestSourceScope(module), module, isTestScope = true)

    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> =
        module.cacheByClassInvalidatingOnRootModifications(KeyForModulesWhoseInternalsAreVisible::class.java) {
            val list = SmartList<ModuleInfo>()

            list.addIfNotNull(module.productionSourceInfo())

            TestModuleProperties.getInstance(module).productionModule?.let {
                list.addIfNotNull(it.productionSourceInfo())
            }

            list.addAll(list.closure { it.expectedBy })

            list.toHashSet()
        }

    private object KeyForModulesWhoseInternalsAreVisible
}

fun Module.productionSourceInfo(): ModuleProductionSourceInfo? = if (hasProductionRoots()) ModuleProductionSourceInfo(this) else null

fun Module.testSourceInfo(): ModuleTestSourceInfo? = if (hasTestRoots()) ModuleTestSourceInfo(this) else null

internal fun Module.correspondingModuleInfos(): List<ModuleSourceInfo> = listOf(testSourceInfo(), productionSourceInfo()).filterNotNull()

private fun Module.hasProductionRoots() =
    hasRootsOfType(JavaSourceRootType.SOURCE) || hasRootsOfType(SourceKotlinRootType) || (isNewMPPModule && sourceType == SourceType.PRODUCTION)

private fun Module.hasTestRoots() =
    hasRootsOfType(JavaSourceRootType.TEST_SOURCE) || hasRootsOfType(TestSourceKotlinRootType) || (isNewMPPModule && sourceType == SourceType.TEST)

private fun Module.hasRootsOfType(sourceRootType: JpsModuleSourceRootType<*>): Boolean =
    rootManager.contentEntries.any { it.getSourceFolders(sourceRootType).isNotEmpty() }

private abstract class ModuleSourceScope(val module: Module) : GlobalSearchScope(module.project) {
    override fun compare(file1: VirtualFile, file2: VirtualFile) = 0
    override fun isSearchInModuleContent(aModule: Module) = aModule == module
    override fun isSearchInLibraries() = false
}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class ModuleProductionSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is ModuleProductionSourceScope && module == other.module)
    }

    override fun calcHashCode(): Int = 31 * module.hashCode()

    override fun contains(file: VirtualFile) =
        moduleFileIndex.isInSourceContentWithoutInjected(file) && !moduleFileIndex.isInTestSourceContentKotlinAware(file)

    override fun toString() = "ModuleProductionSourceScope($module)"
}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class ModuleTestSourceScope(module: Module) : ModuleSourceScope(module) {
    val moduleFileIndex = ModuleRootManager.getInstance(module).fileIndex

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is ModuleTestSourceScope && module == other.module)
    }

    override fun calcHashCode(): Int = 37 * module.hashCode()

    override fun contains(file: VirtualFile) = moduleFileIndex.isInTestSourceContentKotlinAware(file)

    override fun toString() = "ModuleTestSourceScope($module)"
}

open class LibraryInfo(override val project: Project, val library: Library) : IdeaModuleInfo, LibraryModuleInfo, BinaryModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val name: Name = Name.special("<library ${library.name}>")

    override val displayedName: String
        get() = KotlinIdeaAnalysisBundle.message("library.0", library.name.toString())

    override fun contentScope(): GlobalSearchScope = LibraryWithoutSourceScope(project, library)

    override fun dependencies(): List<IdeaModuleInfo> {
        val result = LinkedHashSet<IdeaModuleInfo>()
        result.add(this)

        val (libraries, sdks) = LibraryDependenciesCache.getInstance(project).getLibrariesAndSdksUsedWith(library)

        sdks.mapTo(result) { SdkInfo(project, it) }
        libraries.filter { it is LibraryEx && !it.isDisposed }.flatMapTo(result) {
            createLibraryInfo(project, it)
        }

        return result.toList()
    }

    override val platform: TargetPlatform
        get() = getLibraryPlatform(project, library)

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = platform.findAnalyzerServices(project)

    override val sourcesModuleInfo: SourceForBinaryModuleInfo
        get() = LibrarySourceInfo(project, library, this)

    override fun getLibraryRoots(): Collection<String> =
        library.getFiles(OrderRootType.CLASSES).mapNotNull(PathUtil::getLocalPath)

    override fun toString() = "LibraryInfo(libraryName=${library.name})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return (other is LibraryInfo && library == other.library)
    }

    override fun hashCode(): Int = 43 * library.hashCode()

}

data class LibrarySourceInfo(override val project: Project, val library: Library, override val binariesModuleInfo: BinaryModuleInfo) :
    IdeaModuleInfo, SourceForBinaryModuleInfo {

    override val name: Name = Name.special("<sources for library ${library.name}>")

    override val displayedName: String
        get() = KotlinIdeaAnalysisBundle.message("sources.for.library.0", library.name.toString())

    override fun sourceScope(): GlobalSearchScope = KotlinSourceFilterScope.librarySources(
        LibrarySourceScope(
            project,
            library
        ), project
    )

    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> {
        return createLibraryInfo(project, library)
    }

    override val platform: TargetPlatform
        get() = binariesModuleInfo.platform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = binariesModuleInfo.analyzerServices

    override fun toString() = "LibrarySourceInfo(libraryName=${library.name})"
}

//TODO: (module refactoring) there should be separate SdkSourceInfo but there are no kotlin source in existing sdks for now :)
data class SdkInfo(override val project: Project, val sdk: Sdk) : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.LIBRARY

    override val name: Name = Name.special("<sdk ${sdk.name}>")

    override val displayedName: String
        get() = KotlinIdeaAnalysisBundle.message("sdk.0", sdk.name)

    override fun contentScope(): GlobalSearchScope = SdkScope(project, sdk)

    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)

    override val platform: TargetPlatform
        get() = JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): provide proper target version

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices
}

object NotUnderContentRootModuleInfo : IdeaModuleInfo {
    override val moduleOrigin: ModuleOrigin
        get() = ModuleOrigin.OTHER

    override val name: Name = Name.special("<special module for files not under source root>")

    override val displayedName: String
        get() = KotlinIdeaAnalysisBundle.message("special.module.for.files.not.under.source.root")

    override val project: Project?
        get() = null

    override fun contentScope() = GlobalSearchScope.EMPTY_SCOPE

    //TODO: (module refactoring) dependency on runtime can be of use here
    override fun dependencies(): List<IdeaModuleInfo> = listOf(this)

    override val platform: TargetPlatform
        get() = DefaultIdeTargetPlatformKindProvider.defaultPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = platform.single().findAnalyzerServices()
}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class LibraryWithoutSourceScope(project: Project, private val library: Library) :
    LibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), arrayOf<VirtualFile>()) {

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getClassRootForFile(file)

    override fun equals(other: Any?) = other is LibraryWithoutSourceScope && library == other.library

    override fun calcHashCode(): Int = library.hashCode()

    override fun toString() = "LibraryWithoutSourceScope($library)"
}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class LibrarySourceScope(project: Project, private val library: Library) :
    LibraryScopeBase(project, arrayOf<VirtualFile>(), library.getFiles(OrderRootType.SOURCES)) {

    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getSourceRootForFile(file)

    override fun equals(other: Any?) = other is LibrarySourceScope && library == other.library

    override fun calcHashCode(): Int = library.hashCode()

    override fun toString() = "LibrarySourceScope($library)"
}

//TODO: (module refactoring) android sdk has modified scope
@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class SdkScope(project: Project, val sdk: Sdk) :
    LibraryScopeBase(project, sdk.rootProvider.getFiles(OrderRootType.CLASSES), arrayOf<VirtualFile>()) {

    override fun equals(other: Any?) = other is SdkScope && sdk == other.sdk

    override fun calcHashCode(): Int = sdk.hashCode()

    override fun toString() = "SdkScope($sdk)"
}

fun IdeaModuleInfo.isLibraryClasses() = this is SdkInfo || this is LibraryInfo

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
        if (contentScope === GlobalSearchScope.EMPTY_SCOPE) {
            return contentScope
        }

        val project = contentScope.project
            ?: error("Project is empty for scope $contentScope (${contentScope.javaClass.name})")

        return KotlinSourceFilterScope.libraryClassFiles(contentScope, project)
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

data class PlatformModuleInfo(
    override val platformModule: ModuleSourceInfo,
    private val commonModules: List<ModuleSourceInfo> // NOTE: usually contains a single element for current implementation
) : IdeaModuleInfo, CombinedModuleInfo, TrackableModuleInfo {
    override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>
        get() = platformModule.capabilities

    override fun contentScope() = GlobalSearchScope.union(containedModules.map { it.contentScope() }.toTypedArray())

    override val containedModules: List<ModuleSourceInfo> = listOf(platformModule) + commonModules

    override val project: Project
        get() = platformModule.module.project

    override val platform: TargetPlatform
        get() = platformModule.platform

    override val moduleOrigin: ModuleOrigin
        get() = platformModule.moduleOrigin

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = platform.findAnalyzerServices(platformModule.module.project)

    override fun dependencies() = platformModule.dependencies()

    override val expectedBy: List<ModuleInfo>
        get() = platformModule.expectedBy

    override fun modulesWhoseInternalsAreVisible() = containedModules.flatMap { it.modulesWhoseInternalsAreVisible() }

    override val name: Name = Name.special("<Platform module ${platformModule.name} including ${commonModules.map { it.name }}>")

    override val displayedName: String
        get() = KotlinIdeaAnalysisBundle.message(
            "platform.module.0.including.1",
            platformModule.displayedName,
            commonModules.map { it.displayedName }
        )

    override fun createModificationTracker() = platformModule.createModificationTracker()
}

fun IdeaModuleInfo.projectSourceModules(): List<ModuleSourceInfo>? =
    (this as? ModuleSourceInfo)?.let(::listOf) ?: (this as? PlatformModuleInfo)?.containedModules

enum class SourceType {
    PRODUCTION,
    TEST
}

internal val ModuleSourceInfo.sourceType get() = if (this is ModuleTestSourceInfo) SourceType.TEST else SourceType.PRODUCTION
