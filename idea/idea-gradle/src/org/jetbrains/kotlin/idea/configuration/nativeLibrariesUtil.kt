/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeUIModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.idea.configuration.DependencySubstitute.NoSubstitute
import org.jetbrains.kotlin.idea.configuration.DependencySubstitute.YesSubstitute
import org.jetbrains.kotlin.idea.configuration.KotlinNativeLibraryNameUtil.buildIDELibraryName
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.library.lite.LiteKonanLibraryFacade
import org.jetbrains.plugins.gradle.ExternalDependencyId
import org.jetbrains.plugins.gradle.model.DefaultExternalMultiLibraryDependency
import org.jetbrains.plugins.gradle.model.ExternalDependency
import org.jetbrains.plugins.gradle.model.ExternalLibraryDependency
import org.jetbrains.plugins.gradle.model.ExternalMultiLibraryDependency
import org.jetbrains.plugins.gradle.model.FileCollectionDependency
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File

// KT-30490. This `ProjectDataService` must be executed immediately after
// `com.intellij.openapi.externalSystem.service.project.manage.LibraryDataService` to clean-up KLIBs before any other actions taken on them.
@Order(ExternalSystemConstants.BUILTIN_LIBRARY_DATA_SERVICE_ORDER + 1) // force order
class KotlinNativeLibraryDataService : AbstractProjectDataService<LibraryData, Library>() {
    override fun getTargetDataKey() = ProjectKeys.LIBRARY

    // See also `com.intellij.openapi.externalSystem.service.project.manage.LibraryDataService.postProcess()`
    override fun postProcess(
        toImport: MutableCollection<DataNode<LibraryData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        if (projectData == null || modelsProvider is IdeUIModifiableModelsProvider) return

        val librariesModel = modelsProvider.modifiableProjectLibrariesModel
        val potentialOrphans = HashMap<String, Library>()

        librariesModel.libraries.forEach { library ->
            val libraryName = library.name?.takeIf { it.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) } ?: return@forEach
            potentialOrphans[libraryName] = library
        }

        if (potentialOrphans.isEmpty()) return

        modelsProvider.modules.forEach { module ->
            modelsProvider.getOrderEntries(module).forEach inner@{ orderEntry ->
                val libraryOrderEntry = orderEntry as? LibraryOrderEntry ?: return@inner
                if (libraryOrderEntry.isModuleLevel) return@inner

                val libraryName = (libraryOrderEntry.library?.name ?: libraryOrderEntry.libraryName)
                    ?.takeIf { it.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) } ?: return@inner

                potentialOrphans.remove(libraryName)
            }
        }

        potentialOrphans.keys.forEach { libraryName ->
            librariesModel.getLibraryByName(libraryName)?.let { librariesModel.removeLibrary(it) }
        }
    }
}

// KT-29613, KT-29783
internal class KotlinNativeLibrariesDependencySubstitutor(
    mppModel: KotlinMPPGradleModel,
    private val gradleModule: IdeaModule,
    private val resolverCtx: ProjectResolverContext
) {
    // Substitutes `ExternalDependency` entries that represent KLIBs with new dependency entries with proper type and name:
    // - every `FileCollectionDependency` is checked whether it points to an existing KLIB, and substituted if it is
    // - similarly for every `ExternalLibraryDependency` with `groupId == "Kotlin/Native"` (legacy KLIB provided by Gradle plugin <= 1.3.20)
    fun substituteDependencies(dependencies: Collection<ExternalDependency>): List<ExternalDependency> {
        val result = ArrayList(dependencies)
        for (i in 0 until result.size) {
            val dependency = result[i]
            val dependencySubstitute = when (dependency) {
                is FileCollectionDependency -> getFileCollectionDependencySubstitute(dependency)
                is ExternalLibraryDependency -> getExternalLibraryDependencySubstitute(dependency)
                else -> NoSubstitute
            }

            val newDependency = (dependencySubstitute as? YesSubstitute)?.substitute ?: continue
            result[i] = newDependency
        }
        return result
    }

    private val ProjectResolverContext.dependencySubstitutionCache
        get() = getUserData(KLIB_DEPENDENCY_SUBSTITUTION_CACHE) ?: putUserDataIfAbsent(KLIB_DEPENDENCY_SUBSTITUTION_CACHE, HashMap())

    private val libraryProvider = LiteKonanLibraryFacade.getDistributionLibraryProvider(
        mppModel.kotlinNativeHome.takeIf { it != KotlinMPPGradleModel.NO_KOTLIN_NATIVE_HOME }?.let { File(it) }
    )

    private val kotlinVersion: String? by lazy {
        val classpathData = buildClasspathData(gradleModule, resolverCtx)
        val result = findKotlinPluginVersion(classpathData)
        if (result == null) {
            val message = """
                    Unexpectedly can't obtain Kotlin Gradle plugin version for ${gradleModule.name} module.
                    Build classpath is ${classpathData.classpathEntries.flatMap { it.classesFile }}.
                    ${KotlinNativeLibrariesDependencySubstitutor::class.java.simpleName} will run in idle mode. No dependencies will be substituted.

                    Hint: Please make sure that you have explicitly specified version of Kotlin Gradle plugin in `plugins { }` section.
                    Example:

                    plugins {
                        kotlin("multiplatform") version "${bundledRuntimeVersion()}"
                    }
                """.trimIndent()
            if (ApplicationManager.getApplication().isUnitTestMode)
                LOG.warn(message)
            else
                LOG.error(message)
        }
        result
    }

    private fun getFileCollectionDependencySubstitute(dependency: FileCollectionDependency): DependencySubstitute =
        resolverCtx.dependencySubstitutionCache.getOrPut(dependency.id) {
            val libraryFile = dependency.files.firstOrNull() ?: return@getOrPut NoSubstitute
            buildSubstituteIfNecessary(libraryFile)
        }

    private fun getExternalLibraryDependencySubstitute(dependency: ExternalLibraryDependency): DependencySubstitute =
        resolverCtx.dependencySubstitutionCache.getOrPut(dependency.id) {
            if (KOTLIN_NATIVE_LEGACY_GROUP_ID != dependency.group) return@getOrPut NoSubstitute
            val libraryFile = dependency.file ?: return@getOrPut NoSubstitute
            buildSubstituteIfNecessary(libraryFile)
        }

    private fun buildSubstituteIfNecessary(libraryFile: File): DependencySubstitute {
        // need to check whether `library` points to a real KLIB,
        // and if answer is yes then build a new dependency that will substitute original one
        val library = libraryProvider.getLibrary(libraryFile) ?: return NoSubstitute
        val nonNullKotlinVersion = kotlinVersion ?: return NoSubstitute

        val newLibraryName = buildIDELibraryName(nonNullKotlinVersion, library.name, library.platform)

        val substitute = DefaultExternalMultiLibraryDependency().apply {
            classpathOrder = if (library.name == KONAN_STDLIB_NAME) -1 else 0 // keep stdlib upper
            name = newLibraryName
            packaging = DEFAULT_PACKAGING
            files += library.path
            sources += library.sourcePaths
            scope = DependencyScope.PROVIDED.name
        }

        return YesSubstitute(substitute)
    }

    companion object {
        private const val DEFAULT_PACKAGING = "jar"

        private val LOG = Logger.getInstance(KotlinNativeLibrariesDependencySubstitutor::class.java)

        private val KLIB_DEPENDENCY_SUBSTITUTION_CACHE =
            Key.create<MutableMap<ExternalDependencyId, DependencySubstitute>>("KLIB_DEPENDENCY_SUBSTITUTION_CACHE")
    }
}

/**
 * Gradle IDE plugin creates [LibraryData] nodes with internal name consisting of two parts:
 * - mandatory "Gradle: " prefix
 * - and library name
 * Then internal name is propagated to IDE [Library] object, and is displayed in IDE as "Gradle: <LIBRARY_NAME>".
 * [KotlinNativeLibrariesFixer] removes "Gradle: " prefix from all [LibraryData] items representing KLIBs to make them
 * look more friendly.
 *
 * Also, [KotlinNativeLibrariesFixer] makes sure that all KLIBs from Kotlin/Native distribution are added to IDE project model
 * as project-level libraries. This is necessary until the appropriate fix in IDEA will be implemented (for details see IDEA-211451).
 */
internal object KotlinNativeLibrariesFixer {
    fun applyTo(ownerNode: DataNode<GradleSourceSetData>, ideProject: DataNode<ProjectData>) {
        for (libraryDependencyNode in ExternalSystemApiUtil.findAll(ownerNode, ProjectKeys.LIBRARY_DEPENDENCY)) {
            val libraryData = libraryDependencyNode.data.target

            // Only KLIBs from Kotlin/Native distribution can have such prefix:
            if (libraryData.internalName.startsWith("$GRADLE_LIBRARY_PREFIX$KOTLIN_NATIVE_LIBRARY_PREFIX")) {
                fixLibraryName(libraryData)
                addLibraryToProjectModel(libraryData, ideProject)
                fixLibraryDependencyLevel(libraryDependencyNode)
            }
        }
    }

    private fun fixLibraryName(libraryData: LibraryData) {
        libraryData.internalName = libraryData.internalName.substringAfter(GRADLE_LIBRARY_PREFIX)
    }

    private fun addLibraryToProjectModel(libraryData: LibraryData, ideProject: DataNode<ProjectData>) {
        GradleProjectResolverUtil.linkProjectLibrary(ideProject, libraryData)
    }

    private fun fixLibraryDependencyLevel(oldDependencyNode: DataNode<LibraryDependencyData>) {
        val oldDependency = oldDependencyNode.data
        if (oldDependency.level == LibraryLevel.PROJECT) return // nothing to do

        val newDependency = LibraryDependencyData(oldDependency.ownerModule, oldDependency.target, LibraryLevel.PROJECT).apply {
            scope = oldDependency.scope
            order = oldDependency.order
            isExported = oldDependency.isExported
        }

        val parentNode = oldDependencyNode.parent ?: return
        val childNodes = oldDependencyNode.children

        val newDependencyNode = parentNode.createChild(oldDependencyNode.key, newDependency)
        for (child in childNodes) {
            newDependencyNode.addChild(child)
        }

        oldDependencyNode.clear(true)
    }
}

private sealed class DependencySubstitute {
    object NoSubstitute : DependencySubstitute()
    class YesSubstitute(val substitute: ExternalMultiLibraryDependency) : DependencySubstitute()
}

object KotlinNativeLibraryNameUtil {
    private val IDE_LIBRARY_NAME_REGEX = Regex("^$KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE([^\\s]+) - ([^\\s]+)( \\[([\\w]+)\\])?$")

    // Builds the name of Kotlin/Native library that is a part of Kotlin/Native distribution
    // as it will be displayed in IDE UI.
    fun buildIDELibraryName(kotlinVersion: String, libraryName: String, platform: String?): String {
        val platformNamePart = platform?.let { " [$it]" }.orEmpty()
        return "$KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE$kotlinVersion - $libraryName$platformNamePart"
    }

    // N.B. Returns null if this is not IDE name of Kotlin/Native library.
    fun parseIDELibraryName(ideLibraryName: String): Triple<String, String, String?>? {
        val match = IDE_LIBRARY_NAME_REGEX.matchEntire(ideLibraryName) ?: return null

        val kotlinVersion = match.groups[1]!!.value
        val libraryName = match.groups[2]!!.value
        val platform = match.groups[4]?.value

        return Triple(kotlinVersion, libraryName, platform)
    }

    fun isGradleLibraryName(ideLibraryName: String) = ideLibraryName.startsWith(GRADLE_LIBRARY_PREFIX)
}

internal const val KOTLIN_NATIVE_LIBRARY_PREFIX = "Kotlin/Native"
private const val KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE = "$KOTLIN_NATIVE_LIBRARY_PREFIX "
private const val KOTLIN_NATIVE_LEGACY_GROUP_ID = KOTLIN_NATIVE_LIBRARY_PREFIX
private const val GRADLE_LIBRARY_PREFIX = "Gradle: "
