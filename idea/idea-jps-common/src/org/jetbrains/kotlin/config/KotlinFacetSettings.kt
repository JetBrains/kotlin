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

package org.jetbrains.kotlin.config

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyBean
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.TargetPlatformVersion
import org.jetbrains.kotlin.platform.compat.toIdePlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.utils.DescriptionAware

@Deprecated("Use IdePlatformKind instead.", level = DeprecationLevel.ERROR)
sealed class TargetPlatformKind<out Version : TargetPlatformVersion>(
    val version: Version,
    val name: String
) : DescriptionAware {
    override val description = "$name ${version.description}"

    class Jvm(version: JvmTarget) : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<JvmTarget>(version, "JVM") {
        companion object {
            private val JVM_PLATFORMS by lazy { JvmTarget.values().map(::Jvm) }
            operator fun get(version: JvmTarget) = JVM_PLATFORMS[version.ordinal]
        }
    }

    object JavaScript : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<TargetPlatformVersion.NoVersion>(
        TargetPlatformVersion.NoVersion,
        "JavaScript"
    )

    object Common : @Suppress("DEPRECATION_ERROR") TargetPlatformKind<TargetPlatformVersion.NoVersion>(
        TargetPlatformVersion.NoVersion,
        "Common (experimental)"
    )
}

object CoroutineSupport {
    @JvmStatic
    fun byCompilerArguments(arguments: CommonCompilerArguments?): LanguageFeature.State =
            byCompilerArgumentsOrNull(arguments) ?: LanguageFeature.Coroutines.defaultState

    fun byCompilerArgumentsOrNull(arguments: CommonCompilerArguments?): LanguageFeature.State? = when (arguments?.coroutinesState) {
        CommonCompilerArguments.ENABLE -> LanguageFeature.State.ENABLED
        CommonCompilerArguments.WARN, CommonCompilerArguments.DEFAULT -> LanguageFeature.State.ENABLED_WITH_WARNING
        CommonCompilerArguments.ERROR -> LanguageFeature.State.ENABLED_WITH_ERROR
        else -> null
    }

    fun byCompilerArgument(argument: String): LanguageFeature.State =
            LanguageFeature.State.values().find { getCompilerArgument(it).equals(argument, ignoreCase = true) }
            ?: LanguageFeature.Coroutines.defaultState

    fun getCompilerArgument(state: LanguageFeature.State): String = when (state) {
        LanguageFeature.State.ENABLED -> "enable"
        LanguageFeature.State.ENABLED_WITH_WARNING -> "warn"
        LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> "error"
    }
}

sealed class VersionView : DescriptionAware {
    abstract val version: LanguageVersion

    object LatestStable : VersionView() {
        override val version: LanguageVersion = RELEASED_VERSION

        override val description: String
            get() = "Latest stable (${version.versionString})"
    }

    class Specific(override val version: LanguageVersion) : VersionView() {
        override val description: String
            get() = version.description

        override fun equals(other: Any?) = other is Specific && version == other.version

        override fun hashCode() = version.hashCode()
    }

    companion object {
        val RELEASED_VERSION by lazy {
            val latestStable = LanguageVersion.LATEST_STABLE
            if (latestStable.isPreRelease()) {
                val versions = LanguageVersion.values()
                val index = versions.indexOf(latestStable)
                versions.getOrNull(index - 1) ?: LanguageVersion.KOTLIN_1_0
            } else latestStable
        }

        fun deserialize(value: String?, isAutoAdvance: Boolean): VersionView {
            if (isAutoAdvance) return LatestStable
            val languageVersion = LanguageVersion.fromVersionString(value)
            return if (languageVersion != null) Specific(languageVersion) else LatestStable
        }
    }
}

var CommonCompilerArguments.languageVersionView: VersionView
    get() = VersionView.deserialize(languageVersion, autoAdvanceLanguageVersion)
    set(value) {
        languageVersion = value.version.versionString
        autoAdvanceLanguageVersion = value == VersionView.LatestStable
    }

var CommonCompilerArguments.apiVersionView: VersionView
    get() = VersionView.deserialize(apiVersion, autoAdvanceApiVersion)
    set(value) {
        apiVersion = value.version.versionString
        autoAdvanceApiVersion = value == VersionView.LatestStable
    }

enum class KotlinModuleKind {
    DEFAULT,
    SOURCE_SET_HOLDER,
    COMPILATION_AND_SOURCE_SET_HOLDER;

    val isNewMPP: Boolean
        get() = this != DEFAULT
}

class KotlinFacetSettings {
    companion object {
        // Increment this when making serialization-incompatible changes to configuration data
        val CURRENT_VERSION = 3
        val DEFAULT_VERSION = 0
    }

    var version = CURRENT_VERSION
    var useProjectSettings: Boolean = true

    var mergedCompilerArguments: CommonCompilerArguments? = null
        private set

    // TODO: Workaround for unwanted facet settings modification on code analysis
    // To be replaced with proper API for settings update (see BaseKotlinCompilerSettings as an example)
    fun updateMergedArguments() {
        val compilerArguments = compilerArguments
        val compilerSettings = compilerSettings

        mergedCompilerArguments = if (compilerArguments != null) {
            copyBean(compilerArguments).apply {
                if (compilerSettings != null) {
                    parseCommandLineArguments(compilerSettings.additionalArgumentsAsList, this)
                }
            }
        }
        else null
    }

    var compilerArguments: CommonCompilerArguments? = null
        set(value) {
            field = value?.unfrozen() as CommonCompilerArguments?
            updateMergedArguments()
        }

    var compilerSettings: CompilerSettings? = null
        set(value) {
            field = value?.unfrozen() as CompilerSettings?
            updateMergedArguments()
        }

    var languageLevel: LanguageVersion?
        get() = compilerArguments?.languageVersion?.let { LanguageVersion.fromFullVersionString(it) }
        set(value) {
            compilerArguments!!.languageVersion = value?.versionString
        }

    var apiLevel: LanguageVersion?
        get() = compilerArguments?.apiVersion?.let { LanguageVersion.fromFullVersionString(it) }
        set(value) {
            compilerArguments!!.apiVersion = value?.versionString
        }

    var targetPlatform: TargetPlatform? = null
        get() {
            // This work-around is required in order to fix importing of the proper JVM target version.
            //TODO(auskov): this hack should be removed after fixing equals in SimplePlatform
            val args = compilerArguments
            val mappedPlatforms = field?.componentPlatforms?.flatMap {
                if (JvmPlatforms.defaultJvmPlatform.first() == it && args != null) (IdePlatformKind.platformByCompilerArguments(args)
                    ?: return null) else listOf(it)
            }?.toSet() ?: return null

            return TargetPlatform(mappedPlatforms)
        }


    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        message = "This accessor is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
        replaceWith = ReplaceWith("targetPlatform"),
        level = DeprecationLevel.ERROR
    )
    fun getPlatform(): org.jetbrains.kotlin.platform.IdePlatform<*, *>? {
        return targetPlatform?.toIdePlatform()
    }

    var coroutineSupport: LanguageFeature.State?
        get() {
            val languageVersion = languageLevel ?: return LanguageFeature.Coroutines.defaultState
            if (languageVersion < LanguageFeature.Coroutines.sinceVersion!!) return LanguageFeature.State.DISABLED
            return CoroutineSupport.byCompilerArgumentsOrNull(compilerArguments)
        }
        set(value) {
            compilerArguments?.coroutinesState = when (value) {
                null -> CommonCompilerArguments.DEFAULT
                LanguageFeature.State.ENABLED -> CommonCompilerArguments.ENABLE
                LanguageFeature.State.ENABLED_WITH_WARNING -> CommonCompilerArguments.WARN
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> CommonCompilerArguments.ERROR
            }
        }

    var implementedModuleNames: List<String> = emptyList()

    var productionOutputPath: String? = null
    var testOutputPath: String? = null

    var kind: KotlinModuleKind = KotlinModuleKind.DEFAULT
    var sourceSetNames: List<String> = emptyList()
    var isTestModule: Boolean = false

    var externalProjectId: String = ""
}

interface KotlinFacetSettingsProvider {
    fun getSettings(module: Module): KotlinFacetSettings?
    fun getInitializedSettings(module: Module): KotlinFacetSettings

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinFacetSettingsProvider::class.java)!!
    }
}
