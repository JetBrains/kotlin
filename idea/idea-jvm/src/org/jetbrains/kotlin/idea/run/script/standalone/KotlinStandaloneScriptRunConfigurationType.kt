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

package org.jetbrains.kotlin.idea.run.script.standalone

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinIcons

class KotlinStandaloneScriptRunConfigurationType : ConfigurationTypeBase(
        "KotlinStandaloneScriptRunConfigurationType",
        "Kotlin script",
        "Run Kotlin script",
        KotlinIcons.SMALL_LOGO_13
) {
    init {
        addFactory(Factory(this))
    }

    private class Factory(type: ConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            return KotlinStandaloneScriptRunConfiguration(project, this, "")
        }
    }

    companion object {
        val instance: KotlinStandaloneScriptRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(KotlinStandaloneScriptRunConfigurationType::class.java)
    }
}
