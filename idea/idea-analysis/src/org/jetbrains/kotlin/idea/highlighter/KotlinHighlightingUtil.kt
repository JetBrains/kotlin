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

import com.intellij.ide.projectView.impl.ProjectRootsUtil.isInTestSource
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.project.NotUnderContentRootModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.core.script.scriptDependencies
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil.isInContent
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.script.getScriptDefinition
import kotlin.script.experimental.location.ScriptExpectedLocation

object KotlinHighlightingUtil {
    fun shouldHighlight(psiElement: PsiElement): Boolean {
        val ktFile = psiElement.containingFile as? KtFile ?: return false

        if (ktFile is KtCodeFragment && ktFile.context != null) {
            return true
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
        if (ScratchFileService.isInScratchRoot(ktFile.virtualFile)) return true
        if (ktFile.virtualFile.scriptDependencies == null) return false

        val scriptScope = getScriptDefinition(ktFile)?.scriptExpectedLocations ?: return false
        return when {
            scriptScope.contains(ScriptExpectedLocation.Everywhere) -> true
            scriptScope.contains(ScriptExpectedLocation.Project)
                    && ProjectRootManager.getInstance(ktFile.project).fileIndex.isInContent(ktFile.virtualFile) -> true
            scriptScope.contains(ScriptExpectedLocation.TestsOnly)
                    && isInTestSource(ktFile) -> true
            else -> return isInContent(
                ktFile,
                scriptScope.contains(ScriptExpectedLocation.SourcesOnly),
                scriptScope.contains(ScriptExpectedLocation.Libraries),
                scriptScope.contains(ScriptExpectedLocation.Libraries),
                scriptScope.contains(ScriptExpectedLocation.Libraries)
            )
        }

    }
}