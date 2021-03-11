/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.gradle

import org.jetbrains.plugins.gradle.model.ExternalDependency
import org.jetbrains.plugins.gradle.model.ModelFactory
import java.io.File
import java.io.Serializable

typealias KotlinDependencyId = Long
typealias KotlinDependency = ExternalDependency

class KotlinDependencyMapper {
    private var currentIndex: KotlinDependencyId = 0
    private val idToDependency = HashMap<KotlinDependencyId, KotlinDependency>()
    private val dependencyToId = HashMap<KotlinDependency, KotlinDependencyId>()

    fun getDependency(id: KotlinDependencyId) = idToDependency[id]

    fun getId(dependency: KotlinDependency): KotlinDependencyId {
        return dependencyToId[dependency] ?: let {
            currentIndex++
            dependencyToId[dependency] = currentIndex
            idToDependency[currentIndex] = dependency
            return currentIndex
        }
    }

    fun toDependencyMap(): Map<KotlinDependencyId, KotlinDependency> = idToDependency
}

fun KotlinDependency.deepCopy(cache: MutableMap<Any, Any>): KotlinDependency {
    val cachedValue = cache[this] as? KotlinDependency
    return if (cachedValue != null) {
        cachedValue
    } else {
        val result = ModelFactory.createCopy(this)
        cache[this] = result
        result
    }
}

interface KotlinModule : Serializable {
    val name: String
    val dependencies: Array<KotlinDependencyId>
    val isTestModule: Boolean
}

interface KotlinSourceSet : KotlinModule {
    val languageSettings: KotlinLanguageSettings
    val sourceDirs: Set<File>
    val resourceDirs: Set<File>
    val actualPlatforms: KotlinPlatformContainer


    /**
     * All source sets that this source set explicitly declared a 'dependsOn' relation to
     */
    val declaredDependsOnSourceSets: Set<String>

    /**
     * The whole transitive closure of source sets this source set depends on.
     * ([declaredDependsOnSourceSets] and their dependencies recursively)
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        "This property might be misleading. " +
                "Replace with 'KotlinSourceSetContainer.resolveAllDependsOnSourceSets' to make intention of " +
                "receiving the full transitive closure explicit",
        level = DeprecationLevel.ERROR
    )
    val dependsOnSourceSets: Set<String>
        get() = allDependsOnSourceSets

    /**
     * The whole transitive closure of source sets this source set depends on.
     * ([declaredDependsOnSourceSets] and their dependencies recursively)
     */
    @Deprecated(
        "This set of source sets might be inconsistent with any KotlinSourceSetContainer different to the one used to build this instance" +
                "Replace with 'KotlinSourceSetContainer.resolveAllDependsOnSourceSets' to get consistent resolution",
        level = DeprecationLevel.WARNING
    )
    val allDependsOnSourceSets: Set<String>


    @Deprecated("Returns single target platform. Use actualPlatforms instead", level = DeprecationLevel.ERROR)
    val platform: KotlinPlatform
        get() = actualPlatforms.platforms.singleOrNull() ?: KotlinPlatform.COMMON


    companion object {
        const val COMMON_MAIN_SOURCE_SET_NAME = "commonMain"
        const val COMMON_TEST_SOURCE_SET_NAME = "commonTest"

        // Note. This method could not be deleted due to usage in KotlinAndroidGradleMPPModuleDataService from IDEA Core
        @Suppress("unused")
        fun commonName(forTests: Boolean) = if (forTests) COMMON_TEST_SOURCE_SET_NAME else COMMON_MAIN_SOURCE_SET_NAME
    }
}

interface KotlinLanguageSettings : Serializable {
    val languageVersion: String?
    val apiVersion: String?
    val isProgressiveMode: Boolean
    val enabledLanguageFeatures: Set<String>
    val experimentalAnnotationsInUse: Set<String>
    val compilerPluginArguments: Array<String>
    val compilerPluginClasspath: Set<File>
    val freeCompilerArgs: Array<String>
}

interface KotlinCompilationOutput : Serializable {
    val classesDirs: Set<File>
    val effectiveClassesDir: File?
    val resourcesDir: File?
}

interface KotlinCompilationArguments : Serializable {
    val defaultArguments: Array<String>
    val currentArguments: Array<String>
}

interface KotlinNativeCompilationExtensions : Serializable {
    val konanTarget: String // represents org.jetbrains.kotlin.konan.target.KonanTarget
}

interface KotlinCompilation : KotlinModule {

    @Deprecated("Use allSourceSets or declaredSourceSets instead")
    val sourceSets: Collection<KotlinSourceSet>
        get() = declaredSourceSets

    /**
     * All source sets participated in this compilation, including those available
     * via dependsOn.
     */
    val allSourceSets: Collection<KotlinSourceSet>

    /**
     * Only directly declared source sets of this compilation, i.e. those which are included
     * into compilations directly.
     *
     * Usually, those are automatically created source sets for automatically created
     * compilations (like jvmMain for JVM compilations) or manually included source sets
     * (like 'jvm().compilations["main"].source(mySourceSet)' )
     */
    val declaredSourceSets: Collection<KotlinSourceSet>

    val output: KotlinCompilationOutput
    val arguments: KotlinCompilationArguments
    val dependencyClasspath: Array<String>
    val disambiguationClassifier: String?
    val platform: KotlinPlatform
    val kotlinTaskProperties: KotlinTaskProperties
    val nativeExtensions: KotlinNativeCompilationExtensions?

    companion object {
        const val MAIN_COMPILATION_NAME = "main"
        const val TEST_COMPILATION_NAME = "test"
    }
}

enum class KotlinPlatform(val id: String) {
    COMMON("common"), // this platform is left only for compatibility with NMPP (should not be used in HMPP)
    JVM("jvm"),
    JS("js"),
    NATIVE("native"),
    ANDROID("androidJvm");

    companion object {
        fun byId(id: String) = values().firstOrNull { it.id == id }
    }
}

interface KotlinPlatformContainer : Serializable, Iterable<KotlinPlatform> {
    /**
     * Distinct collection of Platforms.
     * Keeping 'Collection' as type for binary compatibility
     */
    val platforms: Collection<KotlinPlatform>
    val arePlatformsInitialized: Boolean

    @Deprecated(
        "Ambiguous semantics of 'supports' for COMMON or (ANDROID/JVM) platforms. Use 'platforms' directly to express clear intention",
        level = DeprecationLevel.ERROR
    )
    fun supports(simplePlatform: KotlinPlatform): Boolean

    @Deprecated(
        "Unclear semantics: Use 'platforms' directly to express intention",
        level = DeprecationLevel.ERROR
    )
    fun getSinglePlatform() = platforms.singleOrNull() ?: KotlinPlatform.COMMON

    @Deprecated(
        "Unclear semantics: Use 'pushPlatform' instead",
        ReplaceWith("pushPlatform"),
        level = DeprecationLevel.ERROR
    )
    fun addSimplePlatforms(platforms: Collection<KotlinPlatform>) = pushPlatforms(platforms)

    /**
     * Adds the given [platforms] to this container.
     * Note: If any of the pushed [platforms] is common, then this container will drop all non-common platforms and subsequent invocations
     * to this function will have no further effect.
     */
    fun pushPlatforms(platforms: Iterable<KotlinPlatform>)

    /**
     * @see pushPlatforms
     */
    fun pushPlatforms(vararg platform: KotlinPlatform) {
        pushPlatforms(platform.toList())
    }

    override fun iterator(): Iterator<KotlinPlatform> {
        return platforms.toSet().iterator()
    }
}


interface KotlinTargetJar : Serializable {
    val archiveFile: File?
}

interface KotlinTarget : Serializable {
    val name: String
    val presetName: String?
    val disambiguationClassifier: String?
    val platform: KotlinPlatform
    val compilations: Collection<KotlinCompilation>
    val testRunTasks: Collection<KotlinTestRunTask>
    val nativeMainRunTasks: Collection<KotlinNativeMainRunTask>
    val jar: KotlinTargetJar?
    val konanArtifacts: List<KonanArtifactModel>

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}

interface KotlinRunTask : Serializable {
    val taskName: String
    val compilationName: String
}

interface KotlinTestRunTask : KotlinRunTask

interface KotlinNativeMainRunTask : KotlinRunTask {
    val entryPoint: String
    val debuggable: Boolean
}

interface ExtraFeatures : Serializable {
    val coroutinesState: String?
    val isHMPPEnabled: Boolean
    val isNativeDependencyPropagationEnabled: Boolean
}

interface KotlinMPPGradleModel : KotlinSourceSetContainer, Serializable {
    val dependencyMap: Map<KotlinDependencyId, KotlinDependency>
    val targets: Collection<KotlinTarget>
    val extraFeatures: ExtraFeatures
    val kotlinNativeHome: String
    val kotlinImportingDiagnostics: KotlinImportingDiagnosticsContainer

    @Deprecated("Use 'sourceSetsByName' instead", ReplaceWith("sourceSetsByName"), DeprecationLevel.ERROR)
    val sourceSets: Map<String, KotlinSourceSet>
        get() = sourceSetsByName

    override val sourceSetsByName: Map<String, KotlinSourceSet>

    companion object {
        const val NO_KOTLIN_NATIVE_HOME = ""
    }
}

interface KonanArtifactModel : Serializable {
    val targetName: String // represents org.jetbrains.kotlin.gradle.plugin.KotlinTarget.name, ex: "iosX64", "iosArm64"
    val executableName: String // a base name for the output binary file
    val type: String // represents org.jetbrains.kotlin.konan.target.CompilerOutputKind.name, ex: "PROGRAM", "FRAMEWORK"
    val targetPlatform: String // represents org.jetbrains.kotlin.konan.target.KonanTarget.name
    val file: File // the output binary file
    val buildTaskPath: String
    val runConfiguration: KonanRunConfigurationModel
    val isTests: Boolean
}

interface KonanRunConfigurationModel : Serializable {
    val workingDirectory: String
    val programParameters: List<String>
    val environmentVariables: Map<String, String>

    fun isNotEmpty() = workingDirectory.isNotEmpty() || programParameters.isNotEmpty() || environmentVariables.isNotEmpty()

    companion object {
        const val NO_WORKING_DIRECTORY = ""
        val NO_PROGRAM_PARAMETERS = emptyList<String>()
        val NO_ENVIRONMENT_VARIABLES = emptyMap<String, String>()
    }
}
