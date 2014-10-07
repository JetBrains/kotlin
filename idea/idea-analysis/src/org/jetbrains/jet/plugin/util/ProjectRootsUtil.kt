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

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import kotlin.platform.platformStatic
import org.jetbrains.jet.plugin.util.application.runReadAction
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope

public object ProjectRootsUtil {
    platformStatic
    public fun isInSource(element: PsiElement, includeLibrarySources: Boolean): Boolean {
        return runReadAction { (): Boolean ->
            val containingFile = element.getContainingFile()
            if (containingFile == null) return@runReadAction false

            val virtualFile = containingFile.getVirtualFile()
            if (virtualFile == null) return@runReadAction false

            val project = element.getProject()
            val index = ProjectFileIndex.SERVICE.getInstance(project)
            return@runReadAction JetSourceFilterScope.isInProjectSources(project, virtualFile, index, false, includeLibrarySources)
        }!!
    }

    platformStatic
    public fun isInSource(element: PsiElement): Boolean {
        return isInSource(element, true)
    }
}
