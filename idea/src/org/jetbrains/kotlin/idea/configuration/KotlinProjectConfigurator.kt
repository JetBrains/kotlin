/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.resolve.TargetPlatform

enum class ConfigureKotlinStatus {
    /** Kotlin is correctly configured using this configurator. */
    CONFIGURED,
    /** The configurator is not applicable to the current project type. */
    NON_APPLICABLE,
    /** The configurator is applicable to the current project type and can configure Kotlin automatically. */
    CAN_BE_CONFIGURED,
    /**
     * The configurator is applicable to the current project type and Kotlin is not configured,
     * but the state of the project doesn't allow to configure Kotlin automatically.
     */
    BROKEN
}

interface KotlinProjectConfigurator {

    fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus

    @JvmSuppressWildcards fun configure(project: Project, excludeModules: Collection<Module>)

    val presentableText: String

    val name: String

    val targetPlatform: TargetPlatform

    fun updateLanguageVersion(module: Module, languageVersion: String?, apiVersion: String?, requiredStdlibVersion: ApiVersion, forTests: Boolean)

    fun changeCoroutineConfiguration(module: Module, state: LanguageFeature.State)

    companion object {
        val EP_NAME = ExtensionPointName.create<KotlinProjectConfigurator>("org.jetbrains.kotlin.projectConfigurator")
    }
}
