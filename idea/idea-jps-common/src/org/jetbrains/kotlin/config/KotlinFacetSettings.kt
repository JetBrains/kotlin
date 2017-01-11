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
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.utils.DescriptionAware

sealed class TargetPlatformKind<out Version : DescriptionAware>(
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

    object JavaScript : TargetPlatformKind<NoVersion>(NoVersion, "JavaScript")

    object Common : TargetPlatformKind<NoVersion>(NoVersion, "Common (experimental)")

    companion object {

        val ALL_PLATFORMS: List<TargetPlatformKind<*>> by lazy { Jvm.JVM_PLATFORMS + JavaScript + Common }
    }
}

object NoVersion : DescriptionAware {
    override val description = ""
}

data class KotlinVersionInfo(
        var languageLevel: LanguageVersion? = null,
        var apiLevel: LanguageVersion? = null,
        @get:Transient var targetPlatformKind: TargetPlatformKind<*>? = null
) {
    // To be serialized
    var targetPlatformName: String
        get() = targetPlatformKind?.description ?: ""
        set(value) {
            targetPlatformKind = TargetPlatformKind.ALL_PLATFORMS.firstOrNull { it.description == value }
        }
}

enum class CoroutineSupport(
        override val description: String,
        val compilerArgument: String
) : DescriptionAware {
    ENABLED("Enabled", "enable"),
    ENABLED_WITH_WARNING("Enabled with warning", "warn"),
    DISABLED("Disabled", "error");

    companion object {
        val DEFAULT = ENABLED_WITH_WARNING

        @JvmStatic fun byCompilerArguments(arguments: CommonCompilerArguments?) = when {
            arguments == null -> DEFAULT
            arguments.coroutinesEnable -> ENABLED
            arguments.coroutinesWarn -> ENABLED_WITH_WARNING
            arguments.coroutinesError -> DISABLED
            else -> DEFAULT
        }

        fun byCompilerArgument(argument: String): CoroutineSupport {
            return CoroutineSupport.values().find { it.compilerArgument == argument } ?: DEFAULT
        }
    }
}

class KotlinCompilerInfo {
    // To be serialized
    @Property private var _commonCompilerArguments: CommonCompilerArguments.DummyImpl? = null
    @get:Transient var commonCompilerArguments: CommonCompilerArguments?
        get() = _commonCompilerArguments
        set(value) {
            _commonCompilerArguments = value as? CommonCompilerArguments.DummyImpl
        }
    var k2jsCompilerArguments: K2JSCompilerArguments? = null
    var compilerSettings: CompilerSettings? = null

    @get:Transient var coroutineSupport: CoroutineSupport
        get() = CoroutineSupport.byCompilerArguments(commonCompilerArguments)
        set(value) {
            commonCompilerArguments?.coroutinesEnable = value == CoroutineSupport.ENABLED
            commonCompilerArguments?.coroutinesWarn = value == CoroutineSupport.ENABLED_WITH_WARNING
            commonCompilerArguments?.coroutinesError = value == CoroutineSupport.DISABLED
        }
}

class KotlinFacetSettings {
    companion object {
        // Increment this when making serialization-incompatible changes to configuration data
        val CURRENT_VERSION = 1
        val DEFAULT_VERSION = 0
    }

    var useProjectSettings: Boolean = false

    var versionInfo = KotlinVersionInfo()
    var compilerInfo = KotlinCompilerInfo()
}

interface KotlinFacetSettingsProvider {
    fun getSettings(module: Module): KotlinFacetSettings

    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinFacetSettingsProvider::class.java)!!
    }
}