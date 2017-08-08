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

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.IndexableSetContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager

class KotlinScriptDependenciesIndexableSetContributor : IndexableSetContributor() {

    override fun getAdditionalProjectRootsToIndex(project: Project): Set<VirtualFile> {
        val manager = ScriptDependenciesManager.getInstance(project)
        return (manager.getAllScriptsClasspath() + manager.getAllLibrarySources()).filterTo(LinkedHashSet()) { it.isValid }
    }

    override fun getAdditionalRootsToIndex(): Set<VirtualFile> = emptySet()
}