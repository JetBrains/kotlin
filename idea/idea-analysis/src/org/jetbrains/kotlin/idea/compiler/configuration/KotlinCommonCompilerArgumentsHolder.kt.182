/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.cli.common.arguments.setApiVersionToLanguageVersionIfNeeded
import org.jetbrains.kotlin.config.SettingConstants
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION
import org.jetbrains.kotlin.config.detectVersionAutoAdvance
import org.jetbrains.kotlin.config.dropVersionsIfNecessary

@State(name = KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION, storages = [(Storage(SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE))])
class KotlinCommonCompilerArgumentsHolder(project: Project) : BaseKotlinCompilerSettings<CommonCompilerArguments>(project) {
    override fun getState(): Element {
        return super.getState().apply {
            dropVersionsIfNecessary(settings)
        }
    }

    override fun loadState(state: Element) {
        super.loadState(state)

        update {
            // To fix earlier configurations with incorrect combination of language and API version
            setApiVersionToLanguageVersionIfNeeded()
            detectVersionAutoAdvance()
        }
    }

    override fun createSettings() = CommonCompilerArguments.DummyImpl()

    companion object {
        fun getInstance(project: Project) =
                ServiceManager.getService<KotlinCommonCompilerArgumentsHolder>(project, KotlinCommonCompilerArgumentsHolder::class.java)!!
    }
}
