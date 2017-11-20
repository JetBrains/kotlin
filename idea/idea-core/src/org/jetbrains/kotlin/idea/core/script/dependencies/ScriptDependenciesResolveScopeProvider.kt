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
import org.jetbrains.kotlin.idea.caches.resolve.ScriptDependenciesModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.ScriptDependenciesSourceModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfoByVirtualFile

class ScriptDependenciesResolveScopeProvider : ResolveScopeProvider() {
    override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? {
        val moduleInfo = getModuleInfoByVirtualFile(project, file) ?: return null
        val scriptDependenciesModuleInfo = (moduleInfo as? ScriptDependenciesModuleInfo)
                                           ?: (moduleInfo as? ScriptDependenciesSourceModuleInfo)?.binariesModuleInfo
                                           ?: return null
        return GlobalSearchScope.union(
                arrayOf(
                        GlobalSearchScope.fileScope(project, file),
                        *scriptDependenciesModuleInfo.dependencies().map { it.contentScope() }.toTypedArray()
                )
        )
    }
}