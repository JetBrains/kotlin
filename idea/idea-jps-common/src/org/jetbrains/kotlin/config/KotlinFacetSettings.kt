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
import org.jetbrains.kotlin.platform.isCommon
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

    @Deprecated("Use KotlinFacetSettings.mppVersion.isNewMpp")
    val isNewMPP: Boolean
        get() = this != DEFAULT
}

enum class KotlinMultiplatformVersion(val version: Int) {
    M1(1), // the first implementation of MPP. Aka 1.2.0 MPP
    M2(2), // the "New" MPP. Aka 1.3.0 MPP
    M3(3) // the "Hierarchical" MPP.
}

val KotlinMultiplatformVersion?.isOldMpp: Boolean
    get() = this == KotlinMultiplatformVersion.M1

val KotlinMultiplatformVersion?.isNewMPP: Boolean
    get() = this == KotlinMultiplatformVersion.M2

val KotlinMultiplatformVersion?.isHmpp: Boolean
    get() = this == KotlinMultiplatformVersion.M3

data class ExternalSystemTestTask(val testName: String, val externalSystemProjectId: String, val targetName: String?) {

    fun toStringRepresentation() = "$testName|$externalSystemProjectId|$targetName"

    companion object {
        fun fromStringRepresentation(line: String) =
            line.split("|").let { if (it.size == 3) ExternalSystemTestTask(it[0], it[1], it[2]) else null }
    }

    override fun toString() = "$testName@$externalSystemProjectId [$targetName]"
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
            // This work-around is required in order to fix importing of the proper JVM target version and works only
            // for fully actualized JVM target platform
            //TODO(auskov): this hack should be removed after fixing equals in SimplePlatform
            val args = compilerArguments
            val singleSimplePlatform = field?.componentPlatforms?.singleOrNull()
            if (singleSimplePlatform == JvmPlatforms.defaultJvmPlatform.singleOrNull() && args != null) {
                return IdePlatformKind.platformByCompilerArguments(args)
            }
            return field
        }

    var externalSystemTestTasks: List<ExternalSystemTestTask> = emptyList()

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

    var implementedModuleNames: List<String> = emptyList() // used for first implementation of MPP, aka 'old' MPP
    var dependsOnModuleNames: List<String> = emptyList() // used for New MPP and later implementations

    var productionOutputPath: String? = null
    var testOutputPath: String? = null

    var kind: KotlinModuleKind = KotlinModuleKind.DEFAULT
    var sourceSetNames: List<String> = emptyList()
    var isTestModule: Boolean = false

    var externalProjectId: String = ""

    @Deprecated(message = "Use mppVersion.isHmppEnabled")
    var isHmppEnabled: Boolean = false

    val mppVersion: KotlinMultiplatformVersion?
        get() = when {
            isHmppEnabled -> KotlinMultiplatformVersion.M3
            kind.isNewMPP -> KotlinMultiplatformVersion.M2
            targetPlatform.isCommon() || implementedModuleNames.isNotEmpty() -> KotlinMultiplatformVersion.M1
            else -> null
        }

    var pureKotlinSourceFolders: List<String> = emptyList()
}

interface KotlinFacetSettingsProvider {
    fun getSettings(module: Module): KotlinFacetSettings?
    fun getInitializedSettings(module: Module): KotlinFacetSettings

    companion object {
        fun getInstance(project: Project): KotlinFacetSettingsProvider? {
            if (project.isDisposed) {
                return null
            }
            return ServiceManager.getService(project, KotlinFacetSettingsProvider::class.java)
        }
    }
}
