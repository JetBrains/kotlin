/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManagerImpl
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.asSuccess

class OutsiderFileDependenciesLoader(private val manager: ScriptConfigurationManagerImpl) : ScriptDependenciesLoader {
    override val skipSaveToAttributes: Boolean
        get() = true

    override val skipNotification: Boolean
        get() = true

    override fun loadDependencies(
        firstLoad: Boolean,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ): ScriptCompilationConfigurationResult? {
        val virtualFile = file.virtualFile ?: return null
        val project = file.project

        val fileOrigin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(project, virtualFile) ?: return null
        val psiFileOrigin = PsiManager.getInstance(project).findFile(fileOrigin) as? KtFile ?: return null
        return manager.getConfiguration(psiFileOrigin)?.asSuccess()
    }
}