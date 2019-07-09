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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.core.script.IdeScriptReportSink
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.ScriptsCompilationConfigurationUpdater
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.isRunningInCidrIde
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import kotlin.script.experimental.dependencies.ScriptReport

object KotlinHighlightingUtil {
    fun shouldHighlight(psiElement: PsiElement): Boolean {
        val ktFile = psiElement.containingFile as? KtFile ?: return false

        if (ktFile is KtCodeFragment && ktFile.context != null) {
            return true
        }

        if (OutsidersPsiFileSupportWrapper.isOutsiderFile(ktFile.virtualFile)) {
            val origin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(ktFile.project, ktFile.virtualFile) ?: return false
            val psiFileOrigin = PsiManager.getInstance(ktFile.project).findFile(origin) ?: return false
            return shouldHighlight(psiFileOrigin)
        }

        if (ktFile.isScript()) {
            return shouldHighlightScript(ktFile)
        }

        return ProjectRootsUtil.isInProjectOrLibraryContent(ktFile) && ktFile.getModuleInfo() !is NotUnderContentRootModuleInfo
    }

    fun shouldHighlightErrors(psiElement: PsiElement): Boolean {
        val ktFile = psiElement.containingFile as? KtFile ?: return false
        if (ktFile.isCompiled) {
            return false
        }
        if (ktFile is KtCodeFragment && ktFile.context != null) {
            return true
        }

        if (ktFile.isScript()) {
            return shouldHighlightScript(ktFile)
        }

        return ProjectRootsUtil.isInProjectSource(ktFile)
    }


    @Suppress("DEPRECATION")
    private fun shouldHighlightScript(ktFile: KtFile): Boolean {
        if (isRunningInCidrIde) return false // There is no Java support in CIDR. So do not highlight errors in KTS if running in CIDR.
        if (!ScriptsCompilationConfigurationUpdater.areDependenciesCached(ktFile)) return false
        if (ktFile.virtualFile.getUserData(IdeScriptReportSink.Reports)?.any { it.severity == ScriptReport.Severity.FATAL } == true) {
            return false
        }

        if (!ScriptDefinitionsManager.getInstance(ktFile.project).isReady()) return false

        return ProjectRootsUtil.isInProjectSource(ktFile, includeScriptsOutsideSourceRoots = true)
    }
}