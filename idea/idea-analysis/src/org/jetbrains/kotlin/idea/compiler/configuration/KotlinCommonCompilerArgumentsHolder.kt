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

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.text.VersionComparatorUtil
import org.jdom.Element
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION
import org.jetbrains.kotlin.config.getOption

@State(name = KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION,
       storages = arrayOf(Storage(file = StoragePathMacros.PROJECT_FILE),
                          Storage(file = BaseKotlinCompilerSettings.KOTLIN_COMPILER_SETTINGS_PATH,
                                  scheme = StorageScheme.DIRECTORY_BASED)))
class KotlinCommonCompilerArgumentsHolder : BaseKotlinCompilerSettings<CommonCompilerArguments>() {
    private fun Element.dropElementIfDefault() {
        if (DEFAULT_LANGUAGE_VERSION == getAttribute("value")?.value) {
            detach()
        }
    }

    override fun getState(): Element {
        return super.getState().apply {
            // Do not serialize language/api version if they correspond to the default language version
            getOption("languageVersion")?.dropElementIfDefault()
            getOption("apiVersion")?.dropElementIfDefault()
        }
    }

    override fun loadState(state: Element) {
        super.loadState(state)

        // To fix earlier configurations with incorrect combination of language and API version
        val settings = settings
        if (VersionComparatorUtil.compare(settings.languageVersion, settings.apiVersion) < 0) {
            settings.apiVersion = settings.languageVersion
        }
    }

    override fun createSettings() = CommonCompilerArguments.createDefaultInstance()

    companion object {
        private val DEFAULT_LANGUAGE_VERSION = LanguageVersion.LATEST.versionString

        fun getInstance(project: Project) =
                ServiceManager.getService<KotlinCommonCompilerArgumentsHolder>(project, KotlinCommonCompilerArgumentsHolder::class.java)!!
    }
}
