/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Property
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.File
import java.io.Serializable
import java.lang.reflect.InvocationTargetException
import java.util.*

interface ArgsInfo : Serializable {
    val currentArguments: List<String>
    val defaultArguments: List<String>
    val dependencyClasspath: List<String>
}

data class ArgsInfoImpl(
    override val currentArguments: List<String>,
    override val defaultArguments: List<String>,
    override val dependencyClasspath: List<String>
) : ArgsInfo {

    constructor(argsInfo: ArgsInfo) : this(
        ArrayList(argsInfo.currentArguments),
        ArrayList(argsInfo.defaultArguments),
        ArrayList(argsInfo.dependencyClasspath)
    )
}

typealias CompilerArgumentsBySourceSet = Map<String, ArgsInfo>

/**
 * Creates deep copy in order to avoid holding links to Proxy objects created by gradle tooling api
 */
fun CompilerArgumentsBySourceSet.deepCopy(): CompilerArgumentsBySourceSet {
    val result = HashMap<String, ArgsInfo>()
    this.forEach { key, value -> result[key] = ArgsInfoImpl(value) }
    return result
}

interface KotlinGradleModel : Serializable {
    val hasKotlinPlugin: Boolean
    val compilerArgumentsBySourceSet: CompilerArgumentsBySourceSet
    val coroutines: String?
    val platformPluginId: String?
    val implements: List<String>
    val kotlinTarget: String?
    val kotlinTaskProperties: KotlinTaskPropertiesBySourceSet
    val gradleUserHome: String
}

data class KotlinGradleModelImpl(
    override val hasKotlinPlugin: Boolean,
    override val compilerArgumentsBySourceSet: CompilerArgumentsBySourceSet,
    override val coroutines: String?,
    override val platformPluginId: String?,
    override val implements: List<String>,
    override val kotlinTarget: String? = null,
    override val kotlinTaskProperties: KotlinTaskPropertiesBySourceSet,
    override val gradleUserHome: String
) : KotlinGradleModel

abstract class AbstractKotlinGradleModelBuilder : ModelBuilderService {
    companion object {
        val kotlinCompileJvmTaskClasses = listOf(
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompile_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileWithWorkers_Decorated"
        )

        val kotlinCompileTaskClasses = kotlinCompileJvmTaskClasses + listOf(
            "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompileWithWorkers_Decorated",
            "org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommonWithWorkers_Decorated"
        )
        val platformPluginIds = listOf("kotlin-platform-jvm", "kotlin-platform-js", "kotlin-platform-common")
        val pluginToPlatform = linkedMapOf(
            "kotlin" to "kotlin-platform-jvm",
            "kotlin2js" to "kotlin-platform-js"
        )
        val kotlinPluginIds = listOf("kotlin", "kotlin2js", "kotlin-android")
        val ABSTRACT_KOTLIN_COMPILE_CLASS = "org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile"

        val kotlinProjectExtensionClass = "org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension"
        val kotlinSourceSetClass = "org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet"

        val kotlinPluginWrapper = "org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt"

        private val propertyClassPresent = GradleVersion.current() >= GradleVersion.version("4.3")

        fun Task.getSourceSetName(): String = try {
            val method = javaClass.methods.firstOrNull { it.name.startsWith("getSourceSetName") && it.parameterTypes.isEmpty() }
            val sourceSetName = method?.invoke(this)
            when {
                sourceSetName is String -> sourceSetName
                propertyClassPresent && sourceSetName is Property<*> -> sourceSetName.get() as? String
                else -> null
            }
        } catch (e: InvocationTargetException) {
            null // can be thrown if property is not initialized yet
        } ?: "main"
    }
}

class KotlinGradleModelBuilder : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KotlinGradleModel::class.java.name

    private fun getImplementedProjects(project: Project): List<Project> {
        return listOf("expectedBy", "implement")
            .flatMap { project.configurations.findByName(it)?.dependencies ?: emptySet<Dependency>() }
            .filterIsInstance<ProjectDependency>()
            .mapNotNull { it.dependencyProject }
    }

    // see GradleProjectResolverUtil.getModuleId() in IDEA codebase
    private fun Project.pathOrName() = if (path == ":") name else path

    @Suppress("UNCHECKED_CAST")
    private fun Task.getCompilerArguments(methodName: String): List<String>? {
        return try {
            this[methodName] as? List<String>
        } catch (e: Exception) {
            // No argument accessor method is available
            null
        }
    }

    private fun Task.getDependencyClasspath(): List<String> {
        try {
            val abstractKotlinCompileClass = javaClass.classLoader.loadClass(ABSTRACT_KOTLIN_COMPILE_CLASS)
            val getCompileClasspath = abstractKotlinCompileClass.getDeclaredMethod("getCompileClasspath").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            return (getCompileClasspath.invoke(this) as Collection<File>).map { it.path }
        } catch (e: ClassNotFoundException) {
            // Leave arguments unchanged
        } catch (e: NoSuchMethodException) {
            // Leave arguments unchanged
        } catch (e: InvocationTargetException) {
            // We can safely ignore this exception here as getCompileClasspath() gets called again at a later time
            // Leave arguments unchanged
        }
        return emptyList()
    }

    private fun getCoroutines(project: Project): String? {
        val kotlinExtension = project.extensions.findByName("kotlin") ?: return null
        val experimentalExtension = try {
            kotlinExtension::class.java.getMethod("getExperimental").invoke(kotlinExtension)
        } catch (e: NoSuchMethodException) {
            return null
        }

        return try {
            experimentalExtension::class.java.getMethod("getCoroutines").invoke(experimentalExtension)?.toString()
        } catch (e: NoSuchMethodException) {
            null
        }
    }

    override fun buildAll(modelName: String?, project: Project): KotlinGradleModelImpl? {
        val kotlinPluginId = kotlinPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }
        val platformPluginId = platformPluginIds.singleOrNull { project.plugins.findPlugin(it) != null }
        val target = project.getTarget()

        if (kotlinPluginId == null && platformPluginId == null && target == null) {
            return null
        }

        val compilerArgumentsBySourceSet = LinkedHashMap<String, ArgsInfo>()
        val extraProperties = HashMap<String, KotlinTaskProperties>()


        val kotlinCompileTasks = target?.let { it.compilations ?: emptyList() }
                ?.mapNotNull { compilation -> compilation.getCompileKotlinTaskName(project) }
                ?: (project.getAllTasks(false)[project]?.filter { it.javaClass.name in kotlinCompileTaskClasses } ?: emptyList())

        kotlinCompileTasks.forEach { compileTask ->
            if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

            val sourceSetName = compileTask.getSourceSetName()
            val currentArguments = compileTask.getCompilerArguments("getSerializedCompilerArguments")
                ?: compileTask.getCompilerArguments("getSerializedCompilerArgumentsIgnoreClasspathIssues") ?: emptyList()
            val defaultArguments = compileTask.getCompilerArguments("getDefaultSerializedCompilerArguments").orEmpty()
            val dependencyClasspath = compileTask.getDependencyClasspath()
            compilerArgumentsBySourceSet[sourceSetName] = ArgsInfoImpl(currentArguments, defaultArguments, dependencyClasspath)
            extraProperties.acknowledgeTask(compileTask, null)
        }

        val platform = platformPluginId ?: pluginToPlatform.entries.singleOrNull { project.plugins.findPlugin(it.key) != null }?.value
        val implementedProjects = getImplementedProjects(project)

        return KotlinGradleModelImpl(
            kotlinPluginId != null || platformPluginId != null,
            compilerArgumentsBySourceSet,
            getCoroutines(project),
            platform,
            implementedProjects.map { it.pathOrName() },
            platform ?: kotlinPluginId,
            extraProperties,
            project.gradle.gradleUserHomeDir.absolutePath
        )
    }
}
