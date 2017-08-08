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
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.utils.DescriptionAware

sealed class TargetPlatformKind<out Version : TargetPlatformVersion>(
        val version: Version,
        val name: String
) : DescriptionAware {
    override val description = "$name ${version.description}"

    class Jvm(version: JvmTarget) : TargetPlatformKind<JvmTarget>(version, "JVM") {
        companion object {
            val JVM_PLATFORMS by lazy { JvmTarget.values().map(::Jvm) }

            operator fun get(version: JvmTarget) = JVM_PLATFORMS[version.ordinal]
        }
    }

    object JavaScript : TargetPlatformKind<TargetPlatformVersion.NoVersion>(TargetPlatformVersion.NoVersion, "JavaScript")

    object Common : TargetPlatformKind<TargetPlatformVersion.NoVersion>(TargetPlatformVersion.NoVersion, "Common (experimental)")

    companion object {
        val ALL_PLATFORMS: List<TargetPlatformKind<*>> by lazy { Jvm.JVM_PLATFORMS + JavaScript + Common }
        val DEFAULT_PLATFORM: TargetPlatformKind<*>
            get() = Jvm[JvmTarget.DEFAULT]
    }
}

object CoroutineSupport {
    @JvmStatic
    fun byCompilerArguments(arguments: CommonCompilerArguments?): LanguageFeature.State =
            byCompilerArgumentsOrNull(arguments) ?: LanguageFeature.Coroutines.defaultState

    fun byCompilerArgumentsOrNull(arguments: CommonCompilerArguments?): LanguageFeature.State? = when (arguments?.coroutinesState) {
        CommonCompilerArguments.ENABLE -> LanguageFeature.State.ENABLED
        CommonCompilerArguments.WARN -> LanguageFeature.State.ENABLED_WITH_WARNING
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

class KotlinFacetSettings {
    companion object {
        // Increment this when making serialization-incompatible changes to configuration data
        val CURRENT_VERSION = 3
        val DEFAULT_VERSION = 0
    }

    var version = CURRENT_VERSION
    var useProjectSettings: Boolean = true

    var compilerArguments: CommonCompilerArguments? = null
        set(value) {
            field = value?.unfrozen() as CommonCompilerArguments?
        }
    var compilerSettings: CompilerSettings? = null
        set(value) {
            field = value?.unfrozen() as CompilerSettings?
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

    val targetPlatformKind: TargetPlatformKind<*>?
        get() = compilerArguments?.let {
            when (it) {
                is K2JVMCompilerArguments -> {
                    val jvmTarget = it.jvmTarget ?: JvmTarget.DEFAULT.description
                    TargetPlatformKind.Jvm.JVM_PLATFORMS.firstOrNull { it.version.description >= jvmTarget }
                }
                is K2JSCompilerArguments -> TargetPlatformKind.JavaScript
                is K2MetadataCompilerArguments -> TargetPlatformKind.Common
                else -> null
            }
        }

    var coroutineSupport: LanguageFeature.State
        get() {
            val languageVersion = languageLevel ?: return LanguageFeature.Coroutines.defaultState
            if (languageVersion < LanguageFeature.Coroutines.sinceVersion!!) return LanguageFeature.State.DISABLED
            return CoroutineSupport.byCompilerArguments(compilerArguments)
        }
        set(value) {
            compilerArguments!!.coroutinesState = when (value) {
                LanguageFeature.State.ENABLED -> CommonCompilerArguments.ENABLE
                LanguageFeature.State.ENABLED_WITH_WARNING -> CommonCompilerArguments.WARN
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> CommonCompilerArguments.ERROR
            }
        }
}

fun TargetPlatformKind<*>.createCompilerArguments(init: CommonCompilerArguments.() -> Unit = {}): CommonCompilerArguments {
    val arguments = when (this) {
        is TargetPlatformKind.Jvm -> K2JVMCompilerArguments()
        is TargetPlatformKind.JavaScript -> K2JSCompilerArguments()
        is TargetPlatformKind.Common -> K2MetadataCompilerArguments()
    }

    arguments.init()

    if (arguments is K2JVMCompilerArguments) {
        arguments.jvmTarget = this@createCompilerArguments.version.description
    }

    return arguments
}

interface KotlinFacetSettingsProvider {
    fun getSettings(module: Module): KotlinFacetSettings?
    fun getInitializedSettings(module: Module): KotlinFacetSettings

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinFacetSettingsProvider::class.java)!!
    }
}
