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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File

class ScriptTemplatesFromCompilerSettingsProvider(private val project: Project) : ScriptDefinitionContributor {
    override fun getDefinitions(): List<KotlinScriptDefinition> {
        val kotlinSettings = KotlinCompilerSettings.getInstance(project).settings
        return loadDefinitionsFromTemplates(
                templateClassNames = kotlinSettings.scriptTemplates.split(',', ' '),
                templateClasspath = kotlinSettings.scriptTemplatesClasspath.split(File.pathSeparator).map(::File),
                environment = mapOf(
                        "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File))

        )
    }

    override val id: String = "KotlinCompilerScriptTemplatesSettings"
}

