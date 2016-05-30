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

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.jetbrains.kotlin.script.*
import java.io.File

@Suppress("unused") // project component
class KotlinScriptConfigurationManager(project: Project,
                                       private val scriptDefinitionProvider: KotlinScriptDefinitionProvider,
                                       scriptExtraImportsProvider: KotlinScriptExtraImportsProvider?
) : AbstractProjectComponent(project) {

    private val kotlinEnvVars: Map<String, List<String>> by lazy { generateKotlinScriptClasspathEnvVarsForIdea(myProject) }

    init {
        reloadScriptDefinitions()
        val conn = myProject.messageBus.connect()
        conn.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener.Adapter() {
            override fun after(events: List<VFileEvent>) {
                var anyScriptDefinitionChanged = false
                events.filter { it is VFileEvent }.forEach {
                    it.file?.let {
                        if (!anyScriptDefinitionChanged && isScriptDefinitionConfigFile(it)) {
                            anyScriptDefinitionChanged = true
                        }
                        scriptExtraImportsProvider?.run {
                            if (isExtraImportsConfig(it)) {
                                invalidateExtraImports(it)
                            }
                        }
                    }
                }
                if (anyScriptDefinitionChanged) {
                    reloadScriptDefinitions()
                }
            }
        })
    }

    private fun reloadScriptDefinitions() {
        loadScriptConfigsFromProjectRoot(File(myProject.basePath ?: ".")).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.scriptDefinitions =
                        it.map { KotlinConfigurableScriptDefinition(it, kotlinEnvVars) } + StandardScriptDefinition
            }
        }
    }
}


