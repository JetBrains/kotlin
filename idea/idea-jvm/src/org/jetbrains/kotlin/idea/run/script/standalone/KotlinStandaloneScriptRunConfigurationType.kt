/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run.script.standalone

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinIcons

class KotlinStandaloneScriptRunConfigurationType : ConfigurationTypeBase(
    "KotlinStandaloneScriptRunConfigurationType",
    "Kotlin script",
    "Run Kotlin script",
    KotlinIcons.SMALL_LOGO
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
