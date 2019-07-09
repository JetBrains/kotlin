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
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettingsListener
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionSourceAsContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.loadDefinitionsFromTemplates
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import java.io.File
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class ScriptTemplatesFromCompilerSettingsProvider(
    private val project: Project,
    private val compilerSettings: KotlinCompilerSettings
) : ScriptDefinitionSourceAsContributor {

    init {
        project.messageBus.connect().subscribe(KotlinCompilerSettingsListener.TOPIC, object : KotlinCompilerSettingsListener {
            override fun <T> settingsChanged(newSettings: T) {
                if (newSettings !is CompilerSettings) return

                ScriptDefinitionsManager.getInstance(project).reloadDefinitionsBy(this@ScriptTemplatesFromCompilerSettingsProvider)
            }
        })
    }

    override val definitions: Sequence<ScriptDefinition>
        get() {
            val kotlinSettings = compilerSettings.settings
            return if (kotlinSettings.scriptTemplates.isBlank()) emptySequence()
            else loadDefinitionsFromTemplates(
                templateClassNames = kotlinSettings.scriptTemplates.split(',', ' '),
                templateClasspath = kotlinSettings.scriptTemplatesClasspath.split(File.pathSeparator).map(::File),
                baseHostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration) {
                    getEnvironment {
                        mapOf(
                            "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let(::File)
                        )
                    }
                }
            ).asSequence()
        }

    override val id: String = "KotlinCompilerScriptTemplatesSettings"
}

