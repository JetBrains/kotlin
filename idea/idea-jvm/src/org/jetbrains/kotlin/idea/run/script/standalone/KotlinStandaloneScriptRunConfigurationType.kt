/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run.script.standalone

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinJvmBundle

class KotlinStandaloneScriptRunConfigurationType : SimpleConfigurationType(
    "KotlinStandaloneScriptRunConfigurationType",
    KotlinJvmBundle.message("name.kotlin.script"),
    KotlinJvmBundle.message("run.kotlin.script"),
    NotNullLazyValue.createValue { KotlinIcons.SMALL_LOGO }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return KotlinStandaloneScriptRunConfiguration(project, this, "")
    }

    companion object {
        val instance: KotlinStandaloneScriptRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(KotlinStandaloneScriptRunConfigurationType::class.java)
    }
}
