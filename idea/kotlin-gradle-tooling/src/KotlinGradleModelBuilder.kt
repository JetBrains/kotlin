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
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.HashSet

typealias CompilerArgumentsBySourceSet = Map<String, List<String>>

interface KotlinGradleModel : Serializable {
    val hasKotlinPlugin: Boolean
    val currentCompilerArgumentsBySourceSet: CompilerArgumentsBySourceSet
    val defaultCompilerArgumentsBySourceSet: CompilerArgumentsBySourceSet
    val coroutines: String?
    val platformPluginId: String?
    val transitiveCommonDependencies: Set<String>
}

class KotlinGradleModelImpl(
        override val hasKotlinPlugin: Boolean,
        override val currentCompilerArgumentsBySourceSet: CompilerArgumentsBySourceSet,
        override val defaultCompilerArgumentsBySourceSet: CompilerArgumentsBySourceSet,
        override val coroutines: String?,
        override val platformPluginId: String?,
        override val transitiveCommonDependencies: Set<String>
) : KotlinGradleModel

class KotlinGradleModelBuilder : ModelBuilderService {
    companion object {
        val kotlinCompileTaskClasses = listOf("org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated",
                                              "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile_Decorated")
        val platformPluginIds = listOf("kotlin-platform-jvm", "kotlin-platform-js", "kotlin-platform-common")
        val pluginToPlatform = linkedMapOf(
                "kotlin" to "kotlin-platform-jvm",
                "kotlin2js" to "kotlin-platform-js"
        )
        val kotlinPluginIds = listOf("kotlin", "kotlin2js", "kotlin-android")
        private val kotlinPlatformCommonPluginId = "kotlin-platform-common"
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors").withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KotlinGradleModel::class.java.name

    private fun getImplements(project: Project): Project? {
        val implementsConfiguration = project.configurations.findByName("implement") ?: return null
        val implementsProjectDependency = implementsConfiguration.dependencies.filterIsInstance<ProjectDependency>().firstOrNull()
        return implementsProjectDependency?.dependencyProject
    }

    private fun transitiveCommonDependencies(startingProject: Project): Set<String> {
        val toProcess = LinkedList<Project>()
        toProcess.add(startingProject)
        val processed = HashSet<String>()
        val result = HashSet<String>()
        result.add(startingProject.path)

        while (toProcess.isNotEmpty()) {
            val project = toProcess.pollFirst()
            processed.add(project.path)

            if (!project.plugins.hasPlugin(kotlinPlatformCommonPluginId)) continue

            result.add(project.path)

            val compileConfiguration = project.configurations.findByName("compile") ?: continue
            val dependencies = compileConfiguration
                    .dependencies
                    .filterIsInstance<ProjectDependency>()
                    .map { it.dependencyProject }

            for (dep in dependencies) {
                if (dep.path !in processed) {
                    toProcess.add(dep)
                }
            }
        }

        return result
    }

    private fun Class<*>.findGetterMethod(name: String): Method? {
        generateSequence(this) { it.superclass }.forEach {
            try {
                return it.getDeclaredMethod(name)
            }
            catch(e: Exception) {
                // Check next super class
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectCompilerArguments(
            compileTask: Task,
            methodName: String,
            argumentsBySourceSet: MutableMap<String, List<String>>
    ) {
        val taskClass = compileTask::class.java
        val sourceSetName = try {
            taskClass.findGetterMethod("getSourceSetName\$kotlin_gradle_plugin")?.invoke(compileTask) as? String
        } catch (e : InvocationTargetException) {
            null // can be thrown if property is not initialized yet
        } ?: "main"
        try {
            argumentsBySourceSet[sourceSetName] = taskClass.getDeclaredMethod(methodName).invoke(compileTask) as List<String>
        }
        catch (e : NoSuchMethodException) {
            // No argument accessor method is available
        }
    }

    private fun getCoroutines(project: Project): String? {
        val kotlinExtension = project.extensions.findByName("kotlin") ?: return null
        val experimentalExtension = try {
            kotlinExtension::class.java.getMethod("getExperimental").invoke(kotlinExtension)
        }
        catch(e: NoSuchMethodException) {
            return null
        }

        return try {
            experimentalExtension::class.java.getMethod("getCoroutines").invoke(experimentalExtension)?.toString()
        }
        catch(e: NoSuchMethodException) {
            null
        }
    }

    override fun buildAll(modelName: String?, project: Project): KotlinGradleModelImpl {
        val kotlinPluginId = kotlinPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }
        val platformPluginId = platformPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }

        val currentCompilerArgumentsBySourceSet = LinkedHashMap<String, List<String>>()
        val defaultCompilerArgumentsBySourceSet = LinkedHashMap<String, List<String>>()

        project.getAllTasks(false)[project]?.forEach { compileTask ->
            if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

            collectCompilerArguments(compileTask, "getSerializedCompilerArguments", currentCompilerArgumentsBySourceSet)
            collectCompilerArguments(compileTask, "getDefaultSerializedCompilerArguments", defaultCompilerArgumentsBySourceSet)
        }

        val platform = platformPluginId ?: pluginToPlatform.entries.singleOrNull { project.plugins.findPlugin(it.key) != null }?.value
        val transitiveCommon = getImplements(project)?.let { transitiveCommonDependencies(it) } ?: emptySet()

        return KotlinGradleModelImpl(
                kotlinPluginId != null || platformPluginId != null,
                currentCompilerArgumentsBySourceSet,
                defaultCompilerArgumentsBySourceSet,
                getCoroutines(project),
                platform,
                transitiveCommon
        )
    }
}
