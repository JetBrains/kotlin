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
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.StandardIdeScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KotlinScriptDefinitionFromAnnotatedTemplate

class KotlinScriptResolveScopeProvider : ResolveScopeProvider() {
    companion object {
        // Used in LivePlugin
        val USE_NULL_RESOLVE_SCOPE = "USE_NULL_RESOLVE_SCOPE"
    }

    override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? {
        val scriptDefinition = file.findScriptDefinition(project)
        // TODO: this should get this particular scripts dependencies
        return when {
            scriptDefinition == null -> null
            // This is a workaround for completion in scripts and REPL to provide module dependencies
            scriptDefinition.baseClassType.fromClass == Any::class -> null
            scriptDefinition.asLegacyOrNull<StandardIdeScriptDefinition>() != null -> null
            scriptDefinition is ScriptDefinition.FromConfigurations || scriptDefinition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>() != null -> {
                // TODO: should include the file itself
                ScriptDependenciesManager.getInstance(project).getAllScriptsDependenciesClassFilesScope()
            }
            else -> null
        }
    }
}