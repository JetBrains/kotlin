/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.idea.configuration.DependencySubstitute.NoSubstitute
import org.jetbrains.kotlin.idea.configuration.DependencySubstitute.YesSubstitute
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.lite.LiteKonanLibraryInfoProvider
import org.jetbrains.plugins.gradle.ExternalDependencyId
import org.jetbrains.plugins.gradle.model.DefaultExternalLibraryDependency
import org.jetbrains.plugins.gradle.model.ExternalDependency
import org.jetbrains.plugins.gradle.model.ExternalLibraryDependency
import org.jetbrains.plugins.gradle.model.FileCollectionDependency
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File

// KT-29613, KT-29783
internal class KotlinNativeLibrariesDependencySubstitutor(
    private val mppModel: KotlinMPPGradleModel,
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

    private val libraryInfoProvider by lazy { LiteKonanLibraryInfoProvider(mppModel.kotlinNativeHome) }

    private val kotlinVersion: String? by lazy {
        val classpathData = buildClasspathData(gradleModule, resolverCtx)
        val result = findKotlinPluginVersion(classpathData)
        if (result == null)
            LOG.error(
                """
                    Unexpectedly can't obtain Kotlin Gradle plugin version for ${gradleModule.name} module.
                    Build classpath is ${classpathData.classpathEntries.flatMap { it.classesFile }}.
                    ${KotlinNativeLibrariesDependencySubstitutor::class.java.simpleName} will run in idle mode. No dependencies will be substituted.

                    Hint: Please make sure that you have explicitly specified version of Kotlin Gradle plugin in `plugins { }` section.
                    Example:

                    plugins {
                        kotlin("multiplatform") version "1.3.30"
                    }
                """.trimIndent()
            )

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
        // need to check whether `libraryFile` points to a real KLIB,
        // and if answer is yes then build a new dependency that will substitute original one
        val libraryInfo = libraryInfoProvider.getDistributionLibraryInfo(libraryFile.toPath()) ?: return NoSubstitute
        val nonNullKotlinVersion = kotlinVersion ?: return NoSubstitute

        val platformNamePart = libraryInfo.platform?.let { " [$it]" }.orEmpty()
        val newLibraryName = "$KOTLIN_NATIVE_LIBRARY_PREFIX $nonNullKotlinVersion - ${libraryInfo.name}$platformNamePart"

        val substitute = DefaultExternalLibraryDependency().apply {
            name = newLibraryName
            packaging = DEFAULT_PACKAGING
            file = libraryFile
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

internal object KotlinNativeLibrariesNameFixer {

    // Gradle IDE plugin creates `LibraryData` nodes with internal name consisting of two parts:
    // - mandatory "Gradle: " prefix
    // - and library name
    // Then internal name is propagated to IDE `Library` object, and is displayed in IDE as "Gradle: <LIBRARY_NAME>".
    // KotlinNativeLibrariesNameFixer removes "Gradle: " prefix from all `LibraryData` nodes representing KLIBs.
    fun applyTo(ownerNode: DataNode<GradleSourceSetData>) {
        for (libraryDependency in ExternalSystemApiUtil.findAll(ownerNode, ProjectKeys.LIBRARY_DEPENDENCY)) {
            val libraryData = libraryDependency.data.target
            if (libraryData.internalName.startsWith("$GRADLE_LIBRARY_PREFIX$KOTLIN_NATIVE_LIBRARY_PREFIX")) {
                libraryData.internalName = libraryData.internalName.substringAfter(GRADLE_LIBRARY_PREFIX)
            }
        }
    }
}

private sealed class DependencySubstitute {
    object NoSubstitute : DependencySubstitute()
    class YesSubstitute(val substitute: ExternalLibraryDependency) : DependencySubstitute()
}

private const val KOTLIN_NATIVE_LIBRARY_PREFIX = "Kotlin/Native"
private const val KOTLIN_NATIVE_LEGACY_GROUP_ID = KOTLIN_NATIVE_LIBRARY_PREFIX
private const val GRADLE_LIBRARY_PREFIX = "Gradle: "
