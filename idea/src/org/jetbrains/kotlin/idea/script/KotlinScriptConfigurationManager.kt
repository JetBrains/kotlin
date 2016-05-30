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
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.script.loadScriptDefinitionsFromDirectoryWithConfigs
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class KotlinScriptConfigurationManager(project: Project, scriptDefinitionProvider: KotlinScriptDefinitionProvider) : AbstractProjectComponent(project) {

    val kotlinEnvVars: Map<String, List<String>> by lazy {
        val paths = PathUtil.getKotlinPathsForIdeaPlugin()
        mapOf("kotlin-runtime" to listOf(paths.runtimePath.canonicalPath),
              "project-root" to listOf(project.basePath ?: "."),
              "jdk" to PathUtil.getJdkClassesRoots().map { it.canonicalPath })
    }

    init {
        loadScriptDefinitionsFromDirectoryWithConfigs(File(project.basePath ?: "."), kotlinEnvVars).let {
            if (it.isNotEmpty()) {
                scriptDefinitionProvider.scriptDefinitions = it + StandardScriptDefinition
            }
        }
    }
}

