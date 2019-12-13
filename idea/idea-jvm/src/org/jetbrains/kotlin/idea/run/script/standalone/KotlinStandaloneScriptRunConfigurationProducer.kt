/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run.script.standalone

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class KotlinStandaloneScriptRunConfigurationProducer :
    RunConfigurationProducer<KotlinStandaloneScriptRunConfiguration>(KotlinStandaloneScriptRunConfigurationType.instance) {
    override fun setupConfigurationFromContext(
        configuration: KotlinStandaloneScriptRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        configuration.setupFilePath(pathFromContext(context) ?: return false)
        configuration.setGeneratedName()
        return true
    }

    fun pathFromContext(context: ConfigurationContext?): String? {
        val location = context?.location ?: return null
        return pathFromPsiElement(location.psiElement)
    }

    override fun isConfigurationFromContext(configuration: KotlinStandaloneScriptRunConfiguration, context: ConfigurationContext): Boolean {
        val filePath = configuration.filePath
        return filePath != null && filePath == pathFromContext(context)
    }

    companion object {
        fun pathFromPsiElement(element: PsiElement): String? {
            val file = element.getParentOfType<KtFile>(false) ?: return null
            val script = file.script ?: return null
            return script.containingKtFile.virtualFile.canonicalPath
        }
    }
}
