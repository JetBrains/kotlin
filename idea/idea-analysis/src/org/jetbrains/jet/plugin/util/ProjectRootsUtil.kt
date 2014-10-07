/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager
import kotlin.platform.platformStatic
import org.jetbrains.jet.plugin.util.application.runReadAction

public object ProjectRootsUtil {
    platformStatic
    private fun isInSource(element: PsiElement): Boolean {
        return isInSource(element, true)
    }

    platformStatic
    public fun isInSource(element: PsiElement, includeLibrarySources: Boolean): Boolean {
        return runReadAction { (): Boolean ->
            val containingFile = element.getContainingFile()
            if (containingFile == null) return@runReadAction false

            val virtualFile = containingFile.getVirtualFile()
            if (virtualFile == null) return@runReadAction false

            val index = ProjectFileIndex.SERVICE.getInstance(element.getProject())
            val isInSourceRoot = if (includeLibrarySources)
                index.isInSource(virtualFile)
            else
                index.isInSourceContent(virtualFile)

            if (!isInSourceRoot) return@runReadAction false

            return@runReadAction !JetModuleTypeManager.getInstance()!!.isKtFileInGradleProjectInWrongFolder(element)
        }!!
    }

    platformStatic
    public fun isInSourceWithGradleCheck(element: PsiElement): Boolean {
        return isInSource(element) && !JetModuleTypeManager.getInstance()!!.isKtFileInGradleProjectInWrongFolder(element)
    }
}
