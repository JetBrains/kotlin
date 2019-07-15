/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
    val dependsOnSourceSets: Set<String>
    val actualPlatforms: KotlinPlatformContainer
    @Deprecated("Returns single target platform", ReplaceWith("actualPlatforms.actualPlatforms"), DeprecationLevel.ERROR)
    val platform: KotlinPlatform
        get() = actualPlatforms.getSinglePlatform()


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

interface KotlinCompilation : KotlinModule {
    val sourceSets: Collection<KotlinSourceSet>
    val output: KotlinCompilationOutput
    val arguments: KotlinCompilationArguments
    val dependencyClasspath: Array<String>
    val disambiguationClassifier: String?
    val platform: KotlinPlatform
    val kotlinTaskProperties: KotlinTaskProperties


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

interface KotlinPlatformContainer : Serializable {
    val platforms: Collection<KotlinPlatform>

    fun supports(simplePlatform: KotlinPlatform): Boolean

    fun addSimplePlatforms(platforms: Collection<KotlinPlatform>)

    fun getSinglePlatform() = platforms.singleOrNull() ?: KotlinPlatform.COMMON
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
    val testTasks: Collection<KotlinTestTask>
    val jar: KotlinTargetJar?
    val konanArtifacts: List<KonanArtifactModel>

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}

interface KotlinTestTask : Serializable {
    val taskName: String
}

interface ExtraFeatures : Serializable {
    val coroutinesState: String?
    val isHMPPEnabled: Boolean
}

interface KotlinMPPGradleModel : Serializable {
    val dependencyMap: Map<KotlinDependencyId, KotlinDependency>
    val sourceSets: Map<String, KotlinSourceSet>
    val targets: Collection<KotlinTarget>
    val extraFeatures: ExtraFeatures
    val kotlinNativeHome: String

    companion object {
        const val NO_KOTLIN_NATIVE_HOME = ""
    }
}

interface KonanArtifactModel : Serializable {
    val targetName: String
    val executableName: String
    val type: String // represents org.jetbrains.kotlin.konan.target.CompilerOutputKind
    val targetPlatform: String
    val file: File
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
