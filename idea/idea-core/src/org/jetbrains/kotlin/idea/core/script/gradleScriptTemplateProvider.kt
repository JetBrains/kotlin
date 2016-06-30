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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.gradle.tooling.ProjectConnection
import org.jetbrains.kotlin.script.ScriptTemplateProvider
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import java.io.File

class GradleScriptTemplateProvider(project: Project): ScriptTemplateProvider {

    private val gradleExeSettings: GradleExecutionSettings? by lazy {
        try {
            ExternalSystemApiUtil.getExecutionSettings<GradleExecutionSettings>(
                    project,
                    com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.toCanonicalPath(project.basePath!!),
                    org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID)
        }
        catch (e: NoClassDefFoundError) {
            // TODO: log warning and consider displaying it to user
            null
        }
        catch (e: ClassNotFoundException) {
            null // see todo above
        }
    }

    override val id: String = "Gradle"
    override val version: Int = 1
    override val isValid: Boolean get() = gradleExeSettings?.gradleHome != null

    override val templateClassName: String = "org.gradle.script.lang.kotlin.KotlinBuildScript"
    override val dependenciesClasspath: Iterable<String> by lazy {
        gradleExeSettings?.gradleHome?.let { File(it, "lib") }
                ?.let { if (it.exists()) it else null }
                ?.listFiles { file -> file.extension == "jar" && depLibsPrefixes.any { file.name.startsWith(it) } }
                    ?.map { it.canonicalPath }
                ?: emptyList()
    }
    override val environment: Map<String, Any?>? by lazy { mapOf(
            "gradleHome" to gradleExeSettings?.gradleHome?.let { File(it) },
            "projectRoot" to (project.basePath ?: project.baseDir.canonicalPath)?.let { File(it) },
            "projectActionExecutor" to { action: (ProjectConnection) -> Unit ->
                GradleExecutionHelper().execute(project.basePath!!, null) { action(it) } },
            "gradleJavaHome" to gradleExeSettings?.javaHome)
    }

    companion object {
        private val depLibsPrefixes = listOf("gradle-script-kotlin", "gradle-core")
    }
}

