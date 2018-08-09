/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.plugins.gradle.model.AbstractExternalDependency
import org.jetbrains.plugins.gradle.model.DefaultExternalProjectDependency
import org.jetbrains.plugins.gradle.model.ExternalDependency
import org.jetbrains.plugins.gradle.model.ExternalProjectDependency
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder
import java.io.File
import java.lang.Exception

class KotlinMPPGradleModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder
            .create(project, e, "Gradle import errors")
            .withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean {
        return modelName == KotlinMPPGradleModel::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        val dependencyResolver = DependencyResolverImpl(
            project,
            false,
            false,
            true,
            SourceSetCachedFinder(project)
        )
        val sourceSets = buildSourceSets(project) ?: return null
        val sourceSetMap = sourceSets.map { it.name to it }.toMap()
        val targets = buildTargets(sourceSetMap, dependencyResolver, project) ?: return null
        computeSourceSetsDeferredInfo(sourceSets, targets)
        val coroutinesState = getCoroutinesState(project)
        return KotlinMPPGradleModelImpl(sourceSetMap, targets, ExtraFeaturesImpl(coroutinesState))
    }

    private fun getCoroutinesState(project: Project): String? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getExperimental = kotlinExt.javaClass.getMethodOrNull("getExperimental") ?: return null
        val experimentalExt = getExperimental(kotlinExt) ?: return null
        val getCoroutines = experimentalExt.javaClass.getMethodOrNull("getCoroutines") ?: return null
        return getCoroutines(experimentalExt) as? String
    }

    private fun buildSourceSets(project: Project): Collection<KotlinSourceSetImpl>? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getSourceSets = kotlinExt.javaClass.getMethodOrNull("getSourceSets") ?: return null
        @Suppress("UNCHECKED_CAST")
        val sourceSets =
            (getSourceSets(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        return sourceSets.mapNotNull { buildSourceSet(it) }
    }

    private fun buildSourceSet(gradleSourceSet: Named): KotlinSourceSetImpl? {
        val sourceSetClass = gradleSourceSet.javaClass
        val getSourceDirSet = sourceSetClass.getMethodOrNull("getKotlin") ?: return null
        val getResourceDirSet = sourceSetClass.getMethodOrNull("getResources") ?: return null
        val getDependsOn = sourceSetClass.getMethodOrNull("getDependsOn") ?: return null
        val sourceDirs = (getSourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()
        val resourceDirs = (getResourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()
        @Suppress("UNCHECKED_CAST")
        val dependsOnSourceSets = (getDependsOn(gradleSourceSet) as? Set<Named>)?.mapTo(LinkedHashSet()) { it.name } ?: emptySet<String>()
        return KotlinSourceSetImpl(gradleSourceSet.name, sourceDirs, resourceDirs, dependsOnSourceSets)
    }

    private fun buildDependencies(
        gradleCompilation: Any,
        dependencyResolver: DependencyResolver,
        configurationNameAccessor: String,
        scope: String,
        project: Project
    ): Collection<KotlinDependency> {
        val gradleCompilationClass = gradleCompilation.javaClass
        val getConfigurationName = gradleCompilationClass.getMethodOrNull(configurationNameAccessor) ?: return emptyList()
        val configurationName = getConfigurationName(gradleCompilation) as? String ?: return emptyList()
        val configuration = project.configurations.findByName(configurationName) ?: return emptyList()
        if (!configuration.isCanBeResolved) return emptyList()
        val artifactsByProjectPath = configuration
            .resolvedConfiguration
            .lenientConfiguration
            .firstLevelModuleDependencies
            .flatMap { it.moduleArtifacts }
            .filter { it.id.componentIdentifier is ProjectComponentIdentifier }
            .groupBy { (it.id.componentIdentifier as ProjectComponentIdentifier).projectPath }
        return dependencyResolver.resolveDependencies(configuration)
            .apply {
                forEach<ExternalDependency?> { (it as? AbstractExternalDependency)?.scope = scope }
            }
            .map {
                if (it !is ExternalProjectDependency || it.configurationName != Dependency.DEFAULT_CONFIGURATION) return@map it
                val artifacts = artifactsByProjectPath[it.projectPath] ?: return@map it
                val classifier = artifacts.mapTo(LinkedHashSet()) { it.classifier }.singleOrNull() ?: return@map it
                DefaultExternalProjectDependency(it).apply {
                    this.classifier = classifier
                }
            }
    }

    private fun buildTargets(
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project
    ): Collection<KotlinTarget>? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getTargets = kotlinExt.javaClass.getMethodOrNull("getTargets") ?: return null
        @Suppress("UNCHECKED_CAST")
        val targets = (getTargets.invoke(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        return targets.mapNotNull { buildTarget(it, sourceSetMap, dependencyResolver, project) }
    }

    private fun buildTarget(
        gradleTarget: Named,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project
    ): KotlinTarget? {
        val targetClass = gradleTarget.javaClass
        val getPlatformType = targetClass.getMethodOrNull("getPlatformType") ?: return null
        val getCompilations = targetClass.getMethodOrNull("getCompilations") ?: return null
        val getDisambiguationClassifier = targetClass.getMethodOrNull("getDisambiguationClassifier") ?: return null
        val platformId = (getPlatformType.invoke(gradleTarget) as? Named)?.name ?: return null
        val platform = KotlinPlatform.byId(platformId) ?: return null
        val disambiguationClassifier = getDisambiguationClassifier(gradleTarget) as? String
        val isAndroid = targetClass.name == KOTLIN_ANDROID_TARGET_CLASS_NAME
        @Suppress("UNCHECKED_CAST")
        val gradleCompilations =
            (getCompilations.invoke(gradleTarget) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        val compilations = gradleCompilations.mapNotNull { buildCompilation(it, sourceSetMap, dependencyResolver, isAndroid, project) }
        val jar = buildTargetJar(gradleTarget, project)
        val target = KotlinTargetImpl(isAndroid, gradleTarget.name, disambiguationClassifier, platform, compilations, jar)
        compilations.forEach { it.target = target }
        return target
    }

    private fun buildTargetJar(gradleTarget: Named, project: Project): KotlinTargetJar? {
        val targetClass = gradleTarget.javaClass
        val getArtifactsTaskName = targetClass.getMethodOrNull("getArtifactsTaskName") ?: return null
        val artifactsTaskName = getArtifactsTaskName(gradleTarget) as? String ?: return null
        val jarTask = project.tasks.findByName(artifactsTaskName) ?: return null
        val jarTaskClass = jarTask.javaClass
        val getArchivePath = jarTaskClass.getMethodOrNull("getArchivePath")
        val archiveFile = getArchivePath?.invoke(jarTask) as? File?
        return KotlinTargetJarImpl(archiveFile)
    }

    private fun buildCompilation(
        gradleCompilation: Named,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        isAndroid: Boolean,
        project: Project
    ): KotlinCompilationImpl? {
        val compilationClass = gradleCompilation.javaClass
        val getKotlinSourceSets = compilationClass.getMethodOrNull("getKotlinSourceSets") ?: return null
        @Suppress("UNCHECKED_CAST")
        val kotlinGradleSourceSets = (getKotlinSourceSets(gradleCompilation) as? Collection<Named>) ?: return null
        val kotlinSourceSets = kotlinGradleSourceSets.mapNotNull { sourceSetMap[it.name] }
        val getCompileKotlinTaskName = compilationClass.getMethodOrNull("getCompileKotlinTaskName") ?: return null
        @Suppress("UNCHECKED_CAST")
        val compileKotlinTaskName = (getCompileKotlinTaskName(gradleCompilation) as? String) ?: return null
        val compileKotlinTask = project.tasks.findByName(compileKotlinTaskName) ?: return null
        val output = buildCompilationOutput(gradleCompilation, compileKotlinTask) ?: return null
        val arguments = buildCompilationArguments(compileKotlinTask) ?: return null
        val dependencyClasspath = buildDependencyClasspath(compileKotlinTask)
        val dependencies = buildDependencies(gradleCompilation, dependencyResolver, project)
        return KotlinCompilationImpl(isAndroid, gradleCompilation.name, kotlinSourceSets, dependencies, output, arguments, dependencyClasspath)
    }

    private fun buildDependencies(
        gradleCompilation: Named,
        dependencyResolver: DependencyResolver,
        project: Project
    ): Set<KotlinDependency> {
        return LinkedHashSet<KotlinDependency>().apply {
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getCompileDependencyConfigurationName", "COMPILE", project
            )
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getRuntimeDependencyConfigurationName", "RUNTIME", project
            )
        }
    }

    private fun buildCompilationArguments(compileKotlinTask: Task): KotlinCompilationArguments? {
        val compileTaskClass = compileKotlinTask.javaClass
        val getCurrentArguments = compileTaskClass.getMethodOrNull("getSerializedCompilerArguments") ?: return null
        val getDefaultArguments = compileTaskClass.getMethodOrNull("getDefaultSerializedCompilerArguments") ?: return null
        @Suppress("UNCHECKED_CAST")
        val currentArguments = getCurrentArguments(compileKotlinTask) as? List<String> ?: return null
        @Suppress("UNCHECKED_CAST")
        val defaultArguments = getDefaultArguments(compileKotlinTask) as? List<String> ?: return null
        return KotlinCompilationArgumentsImpl(defaultArguments, currentArguments)
    }

    private fun buildDependencyClasspath(compileKotlinTask: Task): List<String> {
        val abstractKotlinCompileClass =
            compileKotlinTask.javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.ABSTRACT_KOTLIN_COMPILE_CLASS)
        val getCompileClasspath =
            abstractKotlinCompileClass.getDeclaredMethodOrNull("getCompileClasspath") ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (getCompileClasspath(compileKotlinTask) as? Collection<File>)?.map { it.path } ?: emptyList()
    }

    private fun buildCompilationOutput(
        gradleCompilation: Named,
        compileKotlinTask: Task
    ): KotlinCompilationOutput? {
        val compilationClass = gradleCompilation.javaClass
        val getOutput = compilationClass.getMethodOrNull("getOutput") ?: return null
        val gradleOutput = getOutput(gradleCompilation) ?: return null
        val gradleOutputClass = gradleOutput.javaClass
        val getClassesDirs = gradleOutputClass.getMethodOrNull("getClassesDirs") ?: return null
        val getResourcesDir = gradleOutputClass.getMethodOrNull("getResourcesDir") ?: return null
        val compileKotlinTaskClass = compileKotlinTask.javaClass
        val getDestinationDir = compileKotlinTaskClass.getMethodOrNull("getDestinationDir") ?: return null
        val classesDirs = getClassesDirs(gradleOutput) as? FileCollection ?: return null
        val resourcesDir = getResourcesDir(gradleOutput) as? File ?: return null
        val destinationDir = getDestinationDir(compileKotlinTask) as? File
        return KotlinCompilationOutputImpl(classesDirs.files, destinationDir, resourcesDir)
    }

    private fun computeSourceSetsDeferredInfo(
        sourceSets: Collection<KotlinSourceSetImpl>,
        targets: Collection<KotlinTarget>
    ) {
        val sourceSetToCompilations = LinkedHashMap<KotlinSourceSet, MutableSet<KotlinCompilation>>()
        for (target in targets) {
            for (compilation in target.compilations) {
                for (sourceSet in compilation.sourceSets) {
                    sourceSetToCompilations.getOrPut(sourceSet) { LinkedHashSet() } += compilation
                }
            }
        }
        for (sourceSet in sourceSets) {
            val compilations = sourceSetToCompilations[sourceSet] ?: continue
            sourceSet.platform = compilations.map { it.platform }.distinct().singleOrNull() ?: KotlinPlatform.COMMON
            sourceSet.dependencies = compilations.flatMapTo(LinkedHashSet()) { it.dependencies }
            sourceSet.isTestModule = compilations.all { it.isTestModule }
            sourceSet.isAndroid = compilations.all { it.isAndroid }
        }
    }

    companion object {
        private val KOTLIN_ANDROID_TARGET_CLASS_NAME = "org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget"
    }
}