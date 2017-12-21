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
            context: ConfigurationContext?,
            sourceElement: Ref<PsiElement>?
    ): Boolean {
        configuration.setupFilePath(pathFromContext(context) ?: return false)
        configuration.setGeneratedName()
        return true
    }

    fun pathFromContext(context: ConfigurationContext?): String? {
        val location = context?.location ?: return null
        return pathFromPsiElement(location.psiElement)
    }

    override fun isConfigurationFromContext(configuration: KotlinStandaloneScriptRunConfiguration?, context: ConfigurationContext?): Boolean {
        val filePath = configuration?.filePath
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
