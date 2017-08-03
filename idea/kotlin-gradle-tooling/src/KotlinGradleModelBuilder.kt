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
import org.gradle.tooling.ModelBuilder
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.collections.HashSet

interface ArgsInfo : Serializable {
    val currentArguments: List<String>
    val defaultArguments: List<String>
    val dependencyClasspath: List<String>
}

class ArgsInfoImpl(
        override val currentArguments: List<String>,
        override val defaultArguments: List<String>,
        override val dependencyClasspath: List<String>
) : ArgsInfo

typealias CompilerArgumentsBySourceSet = Map<String, ArgsInfo>

interface KotlinGradleModel : Serializable {
    val hasKotlinPlugin: Boolean
    val compilerArgumentsBySourceSet: CompilerArgumentsBySourceSet
    val coroutines: String?
    val platformPluginId: String?
    val implements: String?
    val transitiveCommonDependencies: Set<String>
}

class KotlinGradleModelImpl(
        override val hasKotlinPlugin: Boolean,
        override val compilerArgumentsBySourceSet: CompilerArgumentsBySourceSet,
        override val coroutines: String?,
        override val platformPluginId: String?,
        override val implements: String?,
        override val transitiveCommonDependencies: Set<String>
) : KotlinGradleModel

abstract class AbstractKotlinGradleModelBuilder : ModelBuilderService {
    companion object {
        val kotlinCompileTaskClasses = listOf("org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated",
                                              "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile_Decorated")
        val platformPluginIds = listOf("kotlin-platform-jvm", "kotlin-platform-js", "kotlin-platform-common")
        val pluginToPlatform = linkedMapOf(
                "kotlin" to "kotlin-platform-jvm",
                "kotlin2js" to "kotlin-platform-js"
        )
        val kotlinPluginIds = listOf("kotlin", "kotlin2js", "kotlin-android")
        val kotlinPlatformCommonPluginId = "kotlin-platform-common"
        val ABSTRACT_KOTLIN_COMPILE_CLASS = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"

        fun Task.getSourceSetName(): String {
            return try {
                javaClass.methods.firstOrNull { it.name.startsWith("getSourceSetName") && it.parameterTypes.isEmpty() }?.invoke(this) as? String
            } catch (e : InvocationTargetException) {
                null // can be thrown if property is not initialized yet
            } ?: "main"
        }
    }
}

class KotlinGradleModelBuilder : AbstractKotlinGradleModelBuilder() {
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
        result.add(startingProject.pathOrName())

        while (toProcess.isNotEmpty()) {
            val project = toProcess.pollFirst()
            processed.add(project.path)

            if (!project.plugins.hasPlugin(kotlinPlatformCommonPluginId)) continue

            result.add(project.pathOrName())

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

    // see GradleProjectResolverUtil.getModuleId() in IDEA codebase
    private fun Project.pathOrName() = if (path == ":") name else path

    @Suppress("UNCHECKED_CAST")
    private fun Task.getCompilerArguments(methodName: String): List<String> {
        return try {
            javaClass.getDeclaredMethod(methodName).invoke(this) as List<String>
        }
        catch (e : NoSuchMethodException) {
            // No argument accessor method is available
            emptyList()
        }
    }

    private fun Task.getDependencyClasspath(): List<String> {
        try {
            val abstractKotlinCompileClass = javaClass.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE_CLASS)
            val getCompileClasspath = abstractKotlinCompileClass.getDeclaredMethod("getCompileClasspath").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            return (getCompileClasspath.invoke(this) as Collection<File>).map { it.path }
        }
        catch(e: ClassNotFoundException) {
            // Leave arguments unchanged
        }
        catch (e: NoSuchMethodException) {
            // Leave arguments unchanged
        }
        catch (e: InvocationTargetException) {
            // We can safely ignore this exception here as getCompileClasspath() gets called again at a later time
            // Leave arguments unchanged
        }
        return emptyList()
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

        val compilerArgumentsBySourceSet = LinkedHashMap<String, ArgsInfo>()

        project.getAllTasks(false)[project]?.forEach { compileTask ->
            if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

            val sourceSetName = compileTask.getSourceSetName()
            val currentArguments = compileTask.getCompilerArguments("getSerializedCompilerArguments")
            val defaultArguments = compileTask.getCompilerArguments("getDefaultSerializedCompilerArguments")
            val dependencyClasspath = compileTask.getDependencyClasspath()
            compilerArgumentsBySourceSet[sourceSetName] = ArgsInfoImpl(currentArguments, defaultArguments, dependencyClasspath)
        }

        val platform = platformPluginId ?: pluginToPlatform.entries.singleOrNull { project.plugins.findPlugin(it.key) != null }?.value
        val implementedProject = getImplements(project)
        val transitiveCommon = implementedProject?.let { transitiveCommonDependencies(it) } ?: emptySet()

        return KotlinGradleModelImpl(
                kotlinPluginId != null || platformPluginId != null,
                compilerArgumentsBySourceSet,
                getCoroutines(project),
                platform,
                implementedProject?.pathOrName(),
                transitiveCommon
        )
    }
}
