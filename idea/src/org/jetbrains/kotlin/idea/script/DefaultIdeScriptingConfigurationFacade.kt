/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptingSupport
import org.jetbrains.kotlin.psi.KtFile
import kotlin.script.experimental.intellij.IdeScriptConfigurationControlFacade

class DefaultIdeScriptingConfigurationFacade : IdeScriptConfigurationControlFacade {
    override fun reloadScriptConfiguration(scriptFile: PsiFile, updateEditorWithoutNotification: Boolean) {

        (scriptFile as? KtFile) ?: error("Should be called with script KtFile, but called with $scriptFile")
        DefaultScriptingSupport.getInstance(scriptFile.project)
            .ensureUpToDatedConfigurationSuggested(
                scriptFile,
                skipNotification = updateEditorWithoutNotification,
                forceSync = true
            )
    }
}