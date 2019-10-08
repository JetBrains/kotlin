/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.caches.project.ScriptDependenciesInfo
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager

class ScriptDependenciesResolveScopeProvider : ResolveScopeProvider() {
    override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? {
        val manager = ScriptConfigurationManager.getInstance(project)
        if (manager.getAllScriptsDependenciesClassFiles().isEmpty()) return null

        if (file !in manager.getAllScriptsDependenciesClassFilesScope() && file !in manager.getAllScriptDependenciesSourcesScope()) {
            return null
        }

        return GlobalSearchScope.union(
            arrayOf(
                GlobalSearchScope.fileScope(project, file),
                *ScriptDependenciesInfo.ForProject(project).dependencies().map { it.contentScope() }.toTypedArray()
            )
        )
    }
}