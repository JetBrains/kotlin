/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.KotlinDependency
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel
import org.jetbrains.kotlin.idea.configuration.buildClasspathData
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.buildIDELibraryName
import org.jetbrains.kotlin.idea.configuration.mpp.KotlinDependenciesPreprocessor
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.plugins.gradle.ExternalDependencyId
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import java.io.File

// KT-29613, KT-29783
internal class KotlinNativeLibrariesDependencySubstitutor(
    private val mppModel: KotlinMPPGradleModel,
    private val gradleModule: IdeaModule,
    private val resolverCtx: ProjectResolverContext
) : KotlinDependenciesPreprocessor {

    override fun invoke(dependencies: Iterable<KotlinDependency>): List<KotlinDependency> {
        return substituteDependencies(dependencies.toList())
    }

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
                else -> DependencySubstitute.NoSubstitute
            }

            val newDependency = (dependencySubstitute as? DependencySubstitute.YesSubstitute)?.substitute ?: continue
            result[i] = newDependency
        }
        return result
    }

    private val ProjectResolverContext.dependencySubstitutionCache
        get() = getUserData(KLIB_DEPENDENCY_SUBSTITUTION_CACHE) ?: putUserDataIfAbsent(KLIB_DEPENDENCY_SUBSTITUTION_CACHE, HashMap())

    private val klibInfoProvider: KlibInfoProvider? by lazy {
        val kotlinNativeHome = mppModel.kotlinNativeHome.takeIf { it != KotlinMPPGradleModel.NO_KOTLIN_NATIVE_HOME }?.let(::File)

        if (kotlinNativeHome == null) {
            LOG.warn(
                """
                    Can't obtain Kotlin/Native home path in Kotlin Gradle plugin.
                    ${KotlinMPPGradleModel::class.java.simpleName} is $mppModel.
                    ${KotlinNativeLibrariesDependencySubstitutor::class.java.simpleName} will run in idle mode. No dependencies will be substituted.
                """.trimIndent()
            )

            null
        } else
            KlibInfoProvider(kotlinNativeHome = kotlinNativeHome)
    }

    private val kotlinVersion: String? by lazy {
        // first, try to figure out Kotlin plugin version by classpath (the default approach)
        val classpathData = buildClasspathData(gradleModule, resolverCtx)
        val versionFromClasspath = findKotlinPluginVersion(classpathData)

        if (versionFromClasspath == null) {
            // then, examine Kotlin MPP Gradle model
            val versionFromModel = mppModel.targets
                .asSequence()
                .flatMap { it.compilations.asSequence() }
                .mapNotNull { it.kotlinTaskProperties.pluginVersion?.takeIf(String::isNotBlank) }
                .firstOrNull()

            if (versionFromModel == null) {
                LOG.warn(
                    """
                        Can't obtain Kotlin Gradle plugin version for ${gradleModule.name} module.
                        Build classpath is ${classpathData.classpathEntries.flatMap { it.classesFile }}.
                        ${KotlinMPPGradleModel::class.java.simpleName} is $mppModel.
                        ${KotlinNativeLibrariesDependencySubstitutor::class.java.simpleName} will run in idle mode. No dependencies will be substituted.
                    """.trimIndent()
                )

                null
            } else
                versionFromModel
        } else
            versionFromClasspath
    }

    private fun getFileCollectionDependencySubstitute(dependency: FileCollectionDependency): DependencySubstitute =
        resolverCtx.dependencySubstitutionCache.getOrPut(dependency.id) {
            val libraryFile = dependency.files.firstOrNull() ?: return@getOrPut DependencySubstitute.NoSubstitute
            buildSubstituteIfNecessary(libraryFile)
        }

    private fun getExternalLibraryDependencySubstitute(dependency: ExternalLibraryDependency): DependencySubstitute =
        resolverCtx.dependencySubstitutionCache.getOrPut(dependency.id) {
            val libraryFile = dependency.file ?: return@getOrPut DependencySubstitute.NoSubstitute
            buildSubstituteIfNecessary(libraryFile)
        }

    private fun buildSubstituteIfNecessary(libraryFile: File): DependencySubstitute {
        // need to check whether `library` points to a real KLIB,
        // and if answer is yes then build a new dependency that will substitute original one
        val klib = klibInfoProvider?.getKlibInfo(libraryFile) ?: return DependencySubstitute.NoSubstitute
        val nonNullKotlinVersion = kotlinVersion ?: return DependencySubstitute.NoSubstitute

        val newLibraryName = when (klib) {
            is NativeDistributionKlibInfo -> buildIDELibraryName(
                nonNullKotlinVersion,
                klib.name,
                listOfNotNull(klib.target?.name)
            )
            is NativeDistributionCommonizedKlibInfo -> buildIDELibraryName(
                nonNullKotlinVersion,
                klib.name,
                klib.commonizedTargets.map { it.name },
                klib.ownTarget?.name
            )
        }

        val substitute = DefaultExternalMultiLibraryDependency().apply {
            classpathOrder = if (klib.name == KONAN_STDLIB_NAME) -1 else 0 // keep stdlib upper
            name = newLibraryName
            packaging = DEFAULT_PACKAGING
            files += klib.path
            sources += klib.sourcePaths
            scope = DependencyScope.PROVIDED.name
        }

        return DependencySubstitute.YesSubstitute(substitute)
    }

    companion object {
        private const val DEFAULT_PACKAGING = "jar"

        private val LOG = Logger.getInstance(KotlinNativeLibrariesDependencySubstitutor::class.java)

        private val KLIB_DEPENDENCY_SUBSTITUTION_CACHE =
            Key.create<MutableMap<ExternalDependencyId, DependencySubstitute>>("KLIB_DEPENDENCY_SUBSTITUTION_CACHE")
    }
}

private sealed class DependencySubstitute {
    object NoSubstitute : DependencySubstitute()
    class YesSubstitute(val substitute: ExternalMultiLibraryDependency) : DependencySubstitute()
}
