/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel.Companion.NO_KOTLIN_NATIVE_HOME
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl
import java.io.File
import java.lang.reflect.Method

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
        val sourceSets = buildSourceSets(dependencyResolver, project) ?: return null
        val sourceSetMap = sourceSets.map { it.name to it }.toMap()
        val targets = buildTargets(sourceSetMap, dependencyResolver, project) ?: return null
        computeSourceSetsDeferredInfo(sourceSets, targets)
        val coroutinesState = getCoroutinesState(project)
        reportUnresolvedDependencies(targets)
        val kotlinNativeHome = KotlinNativeHomeEvaluator.getKotlinNativeHome(project) ?: NO_KOTLIN_NATIVE_HOME
        return KotlinMPPGradleModelImpl(sourceSetMap, targets, ExtraFeaturesImpl(coroutinesState), kotlinNativeHome)
    }

    private fun reportUnresolvedDependencies(targets: Collection<KotlinTarget>) {
        targets
            .asSequence()
            .flatMap { it.compilations.asSequence() }
            .flatMap { it.dependencies.asSequence() }
            .mapNotNull { (it as? UnresolvedExternalDependency)?.failureMessage }
            .toSet()
            .forEach { logger.warn(it) }
    }

    private fun getCoroutinesState(project: Project): String? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getExperimental = kotlinExt.javaClass.getMethodOrNull("getExperimental") ?: return null
        val experimentalExt = getExperimental(kotlinExt) ?: return null
        val getCoroutines = experimentalExt.javaClass.getMethodOrNull("getCoroutines") ?: return null
        return getCoroutines(experimentalExt) as? String
    }

    private fun buildSourceSets(dependencyResolver: DependencyResolver, project: Project): Collection<KotlinSourceSetImpl>? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getSourceSets = kotlinExt.javaClass.getMethodOrNull("getSourceSets") ?: return null
        @Suppress("UNCHECKED_CAST")
        val sourceSets =
            (getSourceSets(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        return sourceSets.mapNotNull { buildSourceSet(it, dependencyResolver, project) }
    }

    private fun buildSourceSet(
        gradleSourceSet: Named,
        dependencyResolver: DependencyResolver,
        project: Project
    ): KotlinSourceSetImpl? {
        val sourceSetClass = gradleSourceSet.javaClass
        val getLanguageSettings = sourceSetClass.getMethodOrNull("getLanguageSettings") ?: return null
        val getSourceDirSet = sourceSetClass.getMethodOrNull("getKotlin") ?: return null
        val getResourceDirSet = sourceSetClass.getMethodOrNull("getResources") ?: return null
        val getDependsOn = sourceSetClass.getMethodOrNull("getDependsOn") ?: return null
        val languageSettings = getLanguageSettings(gradleSourceSet)?.let { buildLanguageSettings(it) } ?: return null
        val sourceDirs = (getSourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()
        val resourceDirs = (getResourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()
        val dependencies = buildSourceSetDependencies(gradleSourceSet, dependencyResolver, project)
        @Suppress("UNCHECKED_CAST")
        val dependsOnSourceSets = (getDependsOn(gradleSourceSet) as? Set<Named>)?.mapTo(LinkedHashSet()) { it.name } ?: emptySet<String>()
        return KotlinSourceSetImpl(gradleSourceSet.name, languageSettings, sourceDirs, resourceDirs, dependencies, dependsOnSourceSets)
    }

    private fun buildLanguageSettings(gradleLanguageSettings: Any): KotlinLanguageSettings? {
        val languageSettingsClass = gradleLanguageSettings.javaClass
        val getLanguageVersion = languageSettingsClass.getMethodOrNull("getLanguageVersion") ?: return null
        val getApiVersion = languageSettingsClass.getMethodOrNull("getApiVersion") ?: return null
        val getProgressiveMode = languageSettingsClass.getMethodOrNull("getProgressiveMode") ?: return null
        val getEnabledLanguageFeatures = languageSettingsClass.getMethodOrNull("getEnabledLanguageFeatures") ?: return null
        val getExperimentalAnnotationsInUse = languageSettingsClass.getMethodOrNull("getExperimentalAnnotationsInUse")
        val getCompilerPluginArguments = languageSettingsClass.getMethodOrNull("getCompilerPluginArguments")
        val getCompilerPluginClasspath = languageSettingsClass.getMethodOrNull("getCompilerPluginClasspath")
        @Suppress("UNCHECKED_CAST")
        return KotlinLanguageSettingsImpl(
            getLanguageVersion(gradleLanguageSettings) as? String,
            getApiVersion(gradleLanguageSettings) as? String,
            getProgressiveMode(gradleLanguageSettings) as? Boolean ?: false,
            getEnabledLanguageFeatures(gradleLanguageSettings) as? Set<String> ?: emptySet(),
            getExperimentalAnnotationsInUse?.invoke(gradleLanguageSettings) as? Set<String> ?: emptySet(),
            getCompilerPluginArguments?.invoke(gradleLanguageSettings) as? List<String> ?: emptyList(),
            (getCompilerPluginClasspath?.invoke(gradleLanguageSettings) as? FileCollection)?.files ?: emptySet()
        )
    }

    private fun buildDependencies(
        dependencyHolder: Any,
        dependencyResolver: DependencyResolver,
        configurationNameAccessor: String,
        scope: String,
        project: Project,
        metadataDependencyTransformationBuilder: MetadataDependencyTransformationBuilder
    ): Collection<KotlinDependency> {
        val dependencyHolderClass = dependencyHolder.javaClass
        val getConfigurationName = dependencyHolderClass.getMethodOrNull(configurationNameAccessor) ?: return emptyList()
        val configurationName = getConfigurationName(dependencyHolder) as? String ?: return emptyList()
        val configuration = project.configurations.findByName(configurationName) ?: return emptyList()
        @Suppress("UnstableApiUsage")
        if (!configuration.isCanBeResolved) return emptyList()

        val dependencyAdjuster =
            DependencyAdjuster(configuration, scope, project, metadataDependencyTransformationBuilder.getTransformations(configurationName))

        val resolvedDependencies = dependencyResolver
            .resolveDependencies(configuration)
            .apply {
                forEach<ExternalDependency?> { (it as? AbstractExternalDependency)?.scope = scope }
            }
            .flatMap { dependencyAdjuster.adjustDependency(it) }
        val singleDependencyFiles = resolvedDependencies.mapNotNullTo(LinkedHashSet<File>()) {
            (it as? FileCollectionDependency)?.files?.singleOrNull()
        }
        // Workaround for duplicated dependencies specified as a file collection (KT-26675)
        // Drop this code when the issue is fixed in the platform
        return resolvedDependencies.filter { dependency ->
            if (dependency !is FileCollectionDependency) return@filter true
            val files = dependency.files
            if (files.size <= 1) return@filter true
            (files.any { it !in singleDependencyFiles })
        }
    }

    private fun buildTargets(
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project
    ): Collection<KotlinTarget>? {
        return project.getTargets()?.mapNotNull { buildTarget(it, sourceSetMap, dependencyResolver, project) }
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
        val getPreset = targetClass.getMethodOrNull("getPreset")
        val targetPresetName: String?
        targetPresetName = try {
            val targetPreset = getPreset?.invoke(gradleTarget)
            val getPresetName = targetPreset?.javaClass?.getMethodOrNull("getName")
            getPresetName?.invoke(targetPreset) as? String
        } catch (e: Throwable) {
            "${e::class.java.name}:${e.message}"
        }
        @Suppress("UNCHECKED_CAST")
        val gradleCompilations =
            (getCompilations.invoke(gradleTarget) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        val compilations = gradleCompilations.mapNotNull {
            buildCompilation(it, disambiguationClassifier, sourceSetMap, dependencyResolver, project)
        }
        val jar = buildTargetJar(gradleTarget, project)
        val target = KotlinTargetImpl(gradleTarget.name, targetPresetName, disambiguationClassifier, platform, compilations, jar)
        compilations.forEach {
            it.disambiguationClassifier = target.disambiguationClassifier
            it.platform = target.platform
        }
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
        classifier: String?,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
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
        val dependencies = buildCompilationDependencies(gradleCompilation, classifier, sourceSetMap, dependencyResolver, project)
        return KotlinCompilationImpl(gradleCompilation.name, kotlinSourceSets, dependencies, output, arguments, dependencyClasspath)
    }

    private fun buildCompilationDependencies(
        gradleCompilation: Named,
        classifier: String?,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project
    ): Set<KotlinDependency> {
        return LinkedHashSet<KotlinDependency>().apply {
            val transformationBuilder = MetadataDependencyTransformationBuilder(gradleCompilation)
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getCompileDependencyConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getRuntimeDependencyConfigurationName", "RUNTIME", project, transformationBuilder
            )
            this += sourceSetMap[compilationFullName(gradleCompilation.name, classifier)]?.dependencies ?: emptySet()
        }
    }

    private class MetadataDependencyTransformationBuilder(val sourceSet: Any) {
        val transformationsMethod = sourceSet.javaClass.getMethodOrNull("getDependenciesTransformation", String::class.java)

        class KotlinMetadataDependencyTransformation(
            val groupId: String?,
            val moduleName: String,
            val projectPath: String?,
            val allVisibleSourceSets: Set<String>,
            val useFilesForSourceSets: Map<String, Iterable<File>>
        ) {
            constructor(
                transformation: Any,
                group: Method,
                module: Method,
                projectPath: Method,
                visibleSourceSets: Method,
                useFilesForSourceSets: Method
            ) : this(
                group(transformation) as String?,
                module(transformation) as String,
                projectPath(transformation) as String?,
                visibleSourceSets(transformation) as Set<String>,
                useFilesForSourceSets(transformation) as Map<String, Iterable<File>>
            )
        }

        fun getTransformations(configurationName: String): Collection<KotlinMetadataDependencyTransformation> {
            val transformations = transformationsMethod?.invoke(sourceSet, configurationName) as? Iterable<Any> ?: return emptyList()
            val transformationClass = transformations.firstOrNull()?.javaClass
                ?: return emptyList()

            val getGroupId = transformationClass.getMethodOrNull("getGroupId") ?: return emptyList()
            val getModuleName = transformationClass.getMethodOrNull("getModuleName") ?: return emptyList()
            val getProjectPath = transformationClass.getMethodOrNull("getProjectPath") ?: return emptyList()
            val getAllVisibleSourceSets = transformationClass.getMethodOrNull("getAllVisibleSourceSets") ?: return emptyList()
            val getUseFilesForSourceSets = transformationClass.getMethodOrNull("getUseFilesForSourceSets") ?: return emptyList()

            return transformations.map { transformation ->
                KotlinMetadataDependencyTransformation(
                    transformation,
                    getGroupId,
                    getModuleName,
                    getProjectPath,
                    getAllVisibleSourceSets,
                    getUseFilesForSourceSets
                )
            }.filter { it.allVisibleSourceSets.isNotEmpty() }
        }
    }


    private fun buildSourceSetDependencies(
        gradleSourceSet: Named,
        dependencyResolver: DependencyResolver,
        project: Project
    ): Set<KotlinDependency> {
        return LinkedHashSet<KotlinDependency>().apply {
            val transformationBuilder = MetadataDependencyTransformationBuilder(gradleSourceSet)
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getApiMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getImplementationMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getCompileOnlyMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getRuntimeOnlyMetadataConfigurationName", "RUNTIME", project, transformationBuilder
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun safelyGetArguments(compileKotlinTask: Task, accessor: Method?) = try {
        accessor?.invoke(compileKotlinTask) as? List<String>
    } catch (e: Exception) {
        logger.info(e.message ?: "Unexpected exception: $e", e)
        null
    } ?: emptyList()

    private fun buildCompilationArguments(compileKotlinTask: Task): KotlinCompilationArguments? {
        val compileTaskClass = compileKotlinTask.javaClass
        val getCurrentArguments = compileTaskClass.getMethodOrNull("getSerializedCompilerArguments")
        val getDefaultArguments = compileTaskClass.getMethodOrNull("getDefaultSerializedCompilerArguments")
        val currentArguments = safelyGetArguments(compileKotlinTask, getCurrentArguments)
        val defaultArguments = safelyGetArguments(compileKotlinTask, getDefaultArguments)
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

    @Suppress("UnstableApiUsage")
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
        val getDestinationDir = compileKotlinTaskClass.getMethodOrNull("getDestinationDir")
        val getOutputFile = compileKotlinTaskClass.getMethodOrNull("getOutputFile")
        val classesDirs = getClassesDirs(gradleOutput) as? FileCollection ?: return null
        val resourcesDir = getResourcesDir(gradleOutput) as? File ?: return null
        @Suppress("UNCHECKED_CAST") val destinationDir =
            getDestinationDir?.invoke(compileKotlinTask) as? File
            //TODO: Hack for KotlinNativeCompile
                ?: (getOutputFile?.invoke(compileKotlinTask) as? Property<File>)?.orNull?.parentFile
                ?: return null
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
            val name = sourceSet.name
            if (name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
                sourceSet.platform = KotlinPlatform.COMMON
                sourceSet.isTestModule = false
                continue
            }
            if (name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME) {
                sourceSet.platform = KotlinPlatform.COMMON
                sourceSet.isTestModule = true
                continue
            }

            val compilations = sourceSetToCompilations[sourceSet]
            if (compilations != null) {
                sourceSet.platform = compilations.map { it.platform }.distinct().singleOrNull() ?: KotlinPlatform.COMMON
                sourceSet.isTestModule = compilations.all { it.isTestModule }
            } else {
                // TODO: change me after design about it
                sourceSet.isTestModule = "Test" in sourceSet.name
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private class DependencyAdjuster(
        private val configuration: Configuration,
        private val scope: String,
        private val project: Project,
        transformations: Collection<MetadataDependencyTransformationBuilder.KotlinMetadataDependencyTransformation>
    ) {
        private val adjustmentMap = HashMap<ExternalDependency, List<ExternalDependency>>()

        private val projectDependencyTransformation =
            transformations.filter { it.projectPath != null }.associateBy { it.projectPath }
        //TODO

        val dependenciesByProjectPath by lazy {
            configuration
                .resolvedConfiguration
                .lenientConfiguration
                .allModuleDependencies
                .mapNotNull { dependency ->
                    val artifact = dependency.moduleArtifacts.firstOrNull {
                        it.id.componentIdentifier is ProjectComponentIdentifier
                    } ?: return@mapNotNull null
                    dependency to artifact
                }
                .groupBy { (it.second.id.componentIdentifier as ProjectComponentIdentifier).projectPath }
        }

        private fun wrapDependency(dependency: ExternalProjectDependency, newConfigurationName: String): ExternalProjectDependency {
            return DefaultExternalProjectDependency(dependency).apply {
                this.configurationName = newConfigurationName

                val nestedDependencies = this.dependencies.flatMap { adjustDependency(it) }
                this.dependencies.clear()
                this.dependencies.addAll(nestedDependencies)
            }
        }

        private val libraryDependencyTransformation =
            transformations.filter { it.projectPath == null }.associateBy { it.groupId to it.moduleName }

        private fun adjustLibraryDependency(dependency: ExternalDependency, parentScope: String? = null): List<ExternalDependency> =
            when (dependency) {
                is ExternalLibraryDependency -> {
                    val replaceFiles = libraryDependencyTransformation[dependency.id.group to dependency.id.name]?.useFilesForSourceSets
                    when {
                        replaceFiles != null -> replaceFiles.flatMap { (sourceSetName, replaceFiles) ->
                            replaceFiles.map { replaceFile ->
                                DefaultExternalLibraryDependency(dependency).apply {
                                    // Transitive dependencies don't have their scope set properly; TODO investigate may be IJ bug?
                                    scope = dependency.scope ?: parentScope

                                    classifier = sourceSetName
                                    file = replaceFile

                                    val adjustedDependencies =
                                        dependency.dependencies.flatMap { adjustDependency(it, dependency.scope ?: parentScope) }

                                    dependencies.clear()
                                    dependencies.addAll(adjustedDependencies)
                                }
                            }
                        }
                        else ->
                            listOf(
                                // Do nothing but set the correct scope for this dependency if needed and adjust recursively:
                                DefaultExternalLibraryDependency(dependency).apply {
                                    scope = dependency.scope ?: parentScope

                                    val adjustedDependencies =
                                        dependency.dependencies.flatMap { adjustDependency(it, dependency.scope ?: parentScope) }

                                    dependencies.clear()
                                    dependencies.addAll(adjustedDependencies)
                                }
                            )
                    }
                }
                else -> listOf(dependency)
            }

        fun adjustDependency(dependency: ExternalDependency, parentScope: String? = null): List<ExternalDependency> {
            return adjustmentMap.getOrPut(dependency) {
                if (dependency !is ExternalProjectDependency)
                    return@getOrPut adjustLibraryDependency(dependency, parentScope)
                if (dependency.configurationName != Dependency.DEFAULT_CONFIGURATION)
                    return@getOrPut listOf(dependency)
                val artifacts = dependenciesByProjectPath[dependency.projectPath] ?: return@getOrPut listOf(dependency)
                val artifactConfiguration = artifacts.mapTo(LinkedHashSet()) {
                    it.first.configuration
                }.singleOrNull() ?: return@getOrPut listOf(dependency)
                val taskGetterName = when (scope) {
                    "COMPILE" -> "getApiElementsConfigurationName"
                    "RUNTIME" -> "getRuntimeElementsConfigurationName"
                    else -> return@getOrPut listOf(dependency)
                }
                val dependencyProject =
                    if (project.rootProject.path == dependency.projectPath)
                        project.rootProject
                    else
                        project.rootProject.getChildProjectByPath(dependency.projectPath)

                val targets = dependencyProject?.getTargets() ?: return@getOrPut listOf(dependency)
                val gradleTarget = targets.firstOrNull {
                    val getter = it.javaClass.getMethodOrNull(taskGetterName) ?: return@firstOrNull false
                    getter(it) == artifactConfiguration
                } ?: return@getOrPut listOf(dependency)
                val classifier = gradleTarget.javaClass.getMethodOrNull("getDisambiguationClassifier")?.invoke(gradleTarget) as? String
                    ?: return@getOrPut listOf(dependency)
                val platformDependency = if (classifier != KotlinTarget.METADATA_TARGET_NAME) {
                    wrapDependency(dependency, compilationFullName(KotlinCompilation.MAIN_COMPILATION_NAME, classifier))
                } else null
                val commonDependencies = if (dependencyProject.path in projectDependencyTransformation) {
                    val visibleSourceSets = projectDependencyTransformation.getValue(dependencyProject.path).allVisibleSourceSets
                    visibleSourceSets.map { sourceSetName -> wrapDependency(dependency, sourceSetName) }
                } else {
                    listOf(wrapDependency(dependency, KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
                }
                return if (platformDependency != null) listOf(platformDependency) + commonDependencies else commonDependencies
            }
        }
    }

    companion object {
        private val logger = Logging.getLogger(KotlinMPPGradleModelBuilder::class.java)

        fun Project.getTargets(): Collection<Named>? {
            val kotlinExt = project.extensions.findByName("kotlin") ?: return null
            val getTargets = kotlinExt.javaClass.getMethodOrNull("getTargets") ?: return null
            @Suppress("UNCHECKED_CAST")
            return (getTargets.invoke(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList()
        }
    }
}

private fun Project.getChildProjectByPath(path: String): Project? {
    var project = this
    for (name in path.split(":").asSequence().drop(1)) {
        project = project.childProjects[name] ?: return null
    }
    return project
}

