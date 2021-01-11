/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import java.io.File
import java.lang.Exception

class AndroidDependencyResolverCompat(private val clazz: Class<*>, private val project: Project) {
    private val instance = clazz.getField("INSTANCE").get("null")

    val androidSourceSetDependencies: Map<String, List<Any>>?
        get() = instance.call("getAndroidSourceSetDependencies", project)
}

class CompatibilityLayer(
    private val project: Project,
    val kotlinExtension: KotlinExtensionCompat,
    val androidDependencyResolver: AndroidDependencyResolverCompat?
) {
    companion object {
        fun create(project: Project): CompatibilityLayer? {
            val extension = project.extensions.findByName("kotlin")?.let { KotlinExtensionCompat(it) } ?: return null
            val extensionClassLoader = extension.javaClass.classLoader

            val resolverClass: Class<*>? = extensionClassLoader
                .loadClass("org.jetbrains.kotlin.gradle.targets.android.internal.AndroidDependencyResolver")

            return CompatibilityLayer(
                project,
                extension,
                resolverClass?.let { AndroidDependencyResolverCompat(it, project) }
            )
        }
    }
}

class KotlinExtensionCompat(private val instance: Any) {
    private val classLoader: ClassLoader = instance.javaClass.classLoader

    val targets: Collection<KotlinTargetCompat>?
        get() = instance.call<NamedDomainObjectCollection<Named>>("getTargets")?.asMap?.values?.map { KotlinTargetCompat(it) }

    val compilations: Collection<KotlinCompilationCompat>?
        get() = instance.call<NamedDomainObjectCollection<Named>>("getCompilations")?.asMap?.values?.map { KotlinCompilationCompat(it) }

    val sourceSets: Collection<KotlinSourceSetCompat>?
        get() = instance.call<NamedDomainObjectCollection<Named>>("getSourceSets")?.asMap?.values?.map { KotlinSourceSetCompat(it) } // wut? why not just call, NDOC inherits Collection
}

class KotlinTargetCompat(private val instance: Any) {
    val platform: KotlinPlatform?
        get() = instance.call<Named>("getPlatformType")?.name?.let { KotlinPlatform.byId(it) }

    val preset: KotlinPresetCompat?
        get() = instance.call("getPreset")

    val compilations: Collection<KotlinCompilationCompat>?
        get() = instance.call("getCompilations")

    val useDisambiguationClassifierAsSourceSetNamePrefix: Boolean
        get() = instance.call("getUseDisambiguationClassifierAsSourceSetNamePrefix") ?: true

    val disambiguationClassifier: String?
        get() = instance.call("getDisambiguationClassifier")

    val overrideDisambiguationClassifierOnIdeImport: String?
        get() = instance.call("getOverrideDisambiguationClassifierOnIdeImport")
}

class KotlinPresetCompat(private val instance: Any) {
    val name: String?
        get() = instance.call("getName")
}

class KotlinCompilationCompat(private val instance: Any, private val project: Project) {
    val compilationTaskName: String?
        get() = instance.call("getCompileKotlinTaskName")

    val compilationTask: Task?
        get() = compilationTaskName?.let { project.tasks.findByName(it) }
}

class KotlinSourceSetCompat(private val instance: Named) {
    val name: String = instance.name

    val languageSettings: KotlinLanguageSettingsCompat?
        get() = instance.call<Any>("getLanguageSettings")?.let { KotlinLanguageSettingsCompat(it) }

    val sourceDirs: SourceDirectorySet?
        get() = instance.call<SourceDirectorySet>("getKotlin")

    val resourceDirs: SourceDirectorySet?
        get() = instance.call<SourceDirectorySet>("getResources")

    val dependsOn: Set<String>?
        get() = instance.call<Set<Named>>("getDependsOn")?.mapTo(mutableSetOf()) { it.name }

    fun getDependenciesTransformations(configurationName: String): Collection<KotlinMetadataDependencyTransformation> {
        val transformations = instance.call<Iterable<Any>>("getDependenciesTransformation", configurationName) ?: return emptyList()

        return transformations
            .mapNotNull { KotlinMetadataDependencyTransformation.constructViaReflection(it) }
            .filter { it.allVisibleSourceSets.isNotEmpty() }
    }

    val apiMetadataConfigurationName: String?
        get() = instance.call("getApiMetadataConfigurationName")

    val implementationMetadataConfigurationName: String?
        get() = instance.call("getImplementationMetadataConfigurationName")

    val compileOnlyMetadataConfigurationName: String?
        get() = instance.call("getCompileOnlyMetadataConfigurationName")

    val runtimeOnlyMetadataConfigurationName: String?
        get() = instance.call("getRuntimeOnlyMetadataConfigurationName")

}

class KotlinMetadataDependencyTransformation(
    val groupId: String?,
    val moduleName: String,
    val projectPath: String?,
    val allVisibleSourceSets: Set<String>,
    val useFilesForSourceSets: Map<String, Iterable<File>>
) {
    companion object {
        fun constructViaReflection(instance: Any): KotlinMetadataDependencyTransformation? {
            return KotlinMetadataDependencyTransformation(
                instance.call("getGroupId") ?: return null,
                instance.call("getModuleName") ?: return null,
                instance.call("getProjectPath") ?: return null,
                instance.call("getAllVisibleSourceSets") ?: return null,
                instance.call("getUseFilesForSourceSets") ?: return null
            )
        }
    }
}

class KotlinLanguageSettingsCompat(private val instance: Any) {
    val languageVersion: String?
        get() = instance.call("getLanguageVersion")

    val apiVersion: String?
        get() = instance.call("getApiVersion")

    val progressiveMode: Boolean
        get() = instance.call("getProgressiveMode") ?: false

    val enabledLanguageFeatures: Set<String>
        get() = instance.call("getEnabledLanguageFeatures") ?: emptySet()

    val experimentalAnnotationsInUse: Set<String>
        get() = instance.call("getExperimentalAnnotationsInUse") ?: emptySet()

    val compilerPluginArguments: List<String>
        get() = instance.call("getCompilerPluginArguments") ?: emptyList()

    val compilerPluginClasspath: FileCollection?
        get() = instance.call("getCompilerPluginClasspath")

    val freeCompilerArgs: List<String>
        get() = instance.call("getFreeCompilerArgs") ?: emptyList()

    fun toKotlinLanguageSettingsImpl(): KotlinLanguageSettingsImpl = KotlinLanguageSettingsImpl(
        languageVersion,
        apiVersion,
        progressiveMode,
        enabledLanguageFeatures,
        experimentalAnnotationsInUse,
        compilerPluginArguments.toTypedArray(),
        compilerPluginClasspath?.files ?: emptySet(),
        freeCompilerArgs.toTypedArray()
    )
}

private inline fun <reified T> Any.call(name: String, vararg arguments: Any?): T? {
    val method = this.javaClass.getMethodOrNull(name, *arguments.mapNotNull { it?.javaClass }.toTypedArray()) ?: return logAndReturnNull("Method $name not found in class ${this.javaClass}")
    return try {
        method.invoke(this, arguments) as? T
    } catch (e: Exception) {
        logAndReturnNull("Unexpected exception")
    }
}

private fun logAndReturnNull(string: String, throwable: Throwable? = null): Nothing? {
    return null
}