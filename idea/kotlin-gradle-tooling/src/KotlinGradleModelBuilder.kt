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

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import java.lang.Exception

interface KotlinGradleModel : Serializable {
    val implements: String?
    val currentCompilerArguments: List<String>?
    val defaultCompilerArguments: List<String>?
    val coroutines: String?
}

class KotlinGradleModelImpl(
        override val implements: String?,
        override val currentCompilerArguments: List<String>?,
        override val defaultCompilerArguments: List<String>?,
        override val coroutines: String?
) : KotlinGradleModel

class KotlinGradleModelBuilder : ModelBuilderService {
    companion object {
        val compileTasks = listOf("compileKotlin", "compileKotlin2Js")
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors").withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KotlinGradleModel::class.java.name

    private fun getImplements(project: Project): String? {
        val implementsConfiguration = project.configurations.findByName("implement")
        if (implementsConfiguration != null) {
            val implementsProjectDependency = implementsConfiguration.dependencies.filterIsInstance<ProjectDependency>().firstOrNull()
            if (implementsProjectDependency != null) return implementsProjectDependency.dependencyProject.path
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCompilerArguments(project: Project, methodName: String): List<String>? {
        val compileTask = compileTasks.mapNotNull { project.getTasksByName(it, false).firstOrNull() }.firstOrNull() ?: return null
        val taskClass = compileTask.javaClass
        return try {
            taskClass.getDeclaredMethod(methodName).invoke(compileTask) as List<String>
        }
        catch (e : NoSuchMethodException) {
            null
        }
    }

    private fun getCoroutines(project: Project): String? {
        val kotlinExtension = project.extensions.findByName("kotlin") ?: return null
        val experimentalExtension = try {
            kotlinExtension.javaClass.getMethod("getExperimental").invoke(kotlinExtension)
        }
        catch(e: NoSuchMethodException) {
            return null
        }

        return try {
            experimentalExtension.javaClass.getMethod("getCoroutines").invoke(experimentalExtension)?.toString()
        }
        catch(e: NoSuchMethodException) {
            null
        }
    }

    override fun buildAll(modelName: String?, project: Project) =
            KotlinGradleModelImpl(
                    getImplements(project),
                    getCompilerArguments(project, "getSerializedCompilerArguments"),
                    getCompilerArguments(project, "getDefaultSerializedCompilerArguments"),
                    getCoroutines(project)
            )
}
