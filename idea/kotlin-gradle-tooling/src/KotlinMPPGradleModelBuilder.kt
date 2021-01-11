/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel.Companion.NO_KOTLIN_NATIVE_HOME
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId
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
        val dependencyMapper = KotlinDependencyMapper()
        val importingContext = MultiplatformModelImportingContextImpl(project)

        importingContext.initializeSourceSets(buildSourceSets(importingContext, dependencyResolver, dependencyMapper) ?: return null)

        val targets = buildTargets(importingContext, dependencyResolver, dependencyMapper)
        importingContext.initializeCompilations(targets.flatMap { it.compilations })
        importingContext.initializeTargets(targets)

        computeSourceSetsDeferredInfo(importingContext)

        val coroutinesState = getCoroutinesState(project)
        val kotlinNativeHome = KotlinNativeHomeEvaluator.getKotlinNativeHome(project) ?: NO_KOTLIN_NATIVE_HOME
        return KotlinMPPGradleModelImpl(
            filterOrphanSourceSets(importingContext),
            importingContext.targets,
            // TODO DISCUSS AT REVIEW: can/should we deduplicate ImportingContext.properties and ExtraFeatures?
            ExtraFeaturesImpl(coroutinesState, importingContext.properties.isHmppEnabled, importingContext.properties.enableNativeDependencyPropagation),
            kotlinNativeHome,
            dependencyMapper.toDependencyMap()
        )
    }

    private fun filterOrphanSourceSets(
        importingContext: MultiplatformModelImportingContext
    ): Map<String, KotlinSourceSetImpl> {
        if (importingContext.properties.importOrphanSourceSets) return importingContext.sourceSetsByNames

        val (orphanSourceSets, nonOrphanSourceSets) = importingContext.sourceSets.partition { importingContext.isOrphanSourceSet(it) }

        orphanSourceSets.forEach {
            logger.warn("[sync warning] Source set \"${it.name}\" is not compiled with any compilation. This source set is not imported in the IDE.")
        }
        return nonOrphanSourceSets.associateBy { it.name }
    }

    private fun getCoroutinesState(project: Project): String? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getExperimental = kotlinExt.javaClass.getMethodOrNull("getExperimental") ?: return null
        val experimentalExt = getExperimental(kotlinExt) ?: return null
        val getCoroutines = experimentalExt.javaClass.getMethodOrNull("getCoroutines") ?: return null
        return getCoroutines(experimentalExt) as? String
    }

    private fun buildSourceSets(
        importingContext: MultiplatformModelImportingContext,
        dependencyResolver: DependencyResolver,
        dependencyMapper: KotlinDependencyMapper
    ): Map<String, KotlinSourceSetImpl> {
        val sourceSets = importingContext.compatServices.kotlinExtension.sourceSets ?: emptyList()

        val allSourceSetsProtosByNames = sourceSets.mapNotNull {
            buildSourceSet(it, dependencyResolver, importingContext, dependencyMapper)
        }.associateBy { it.name }

        val dependsOnCache = HashMap<String, Set<String>>()
        // Some performance optimisation: do not build metadata dependencies if source set is not common
        return if (importingContext.properties.buildMetadataDependencies) {
            allSourceSetsProtosByNames.mapValues { (_, proto) ->
                proto.buildKotlinSourceSetImpl(true, allSourceSetsProtosByNames, dependsOnCache)
            }
        } else {
            val unactualizedSourceSets = allSourceSetsProtosByNames.values.flatMap { it.dependsOnSourceSets }.distinct()
            allSourceSetsProtosByNames.mapValues { (name, proto) ->
                proto.buildKotlinSourceSetImpl(unactualizedSourceSets.contains(name), allSourceSetsProtosByNames, dependsOnCache)
            }
        }
    }

    private fun buildSourceSet(
        sourceSet: KotlinSourceSetCompat,
        dependencyResolver: DependencyResolver,
        importingContext: MultiplatformModelImportingContext,
        dependencyMapper: KotlinDependencyMapper,
    ): KotlinSourceSetProto? {
        val sourceSetDependenciesBuilder: () -> Array<KotlinDependencyId> = {
            buildSourceSetDependencies(sourceSet, dependencyResolver, importingContext)
                .map { dependencyMapper.getId(it) }
                .distinct()
                .toTypedArray()
        }
        return KotlinSourceSetProto(
            sourceSet.name,
            sourceSet.languageSettings?.toKotlinLanguageSettingsImpl() ?: return null,
            sourceSet.sourceDirs?.srcDirs ?: return null,
            sourceSet.resourceDirs?.srcDirs ?: return null,
            sourceSetDependenciesBuilder,
            sourceSet.dependsOn ?: return null
        )
    }

    private fun buildDependencies(
        sourceSet: KotlinSourceSetCompat,
        dependencyResolver: DependencyResolver,
        scope: String,
        configuration: Configuration?,
        project: Project
    ): Collection<KotlinDependency> {
        if (configuration == null || !configuration.isCanBeResolved) return emptyList()

        val dependencyAdjuster =
            DependencyAdjuster(configuration, scope, project, sourceSet.getDependenciesTransformations(configuration.name))

        val resolvedDependencies = dependencyResolver
            .resolveDependencies(configuration)
            .apply {
                forEach<ExternalDependency?> { (it as? AbstractExternalDependency)?.scope = scope }
                forEach<ExternalDependency?> {
                    if (it is DefaultExternalProjectDependency && it.projectDependencyArtifacts !is ArrayList) {
                        it.projectDependencyArtifacts = ArrayList(it.projectDependencyArtifacts)
                    }
                }
            }
            .flatMap { dependencyAdjuster.adjustDependency(it) }
        val singleDependencyFiles = resolvedDependencies.mapNotNullTo(LinkedHashSet()) {
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
        importingContext: MultiplatformModelImportingContext,
        dependencyResolver: DependencyResolver,
        dependencyMapper: KotlinDependencyMapper
    ): Collection<KotlinTarget> {
        return importingContext.compatServices.kotlinExtension.targets
            ?.mapNotNull { buildTarget(importingContext, it, dependencyResolver, dependencyMapper) }
            .orEmpty()
    }

    private operator fun Any?.get(methodName: String, vararg params: Any): Any? {
        return this[methodName, params.map { it.javaClass }, params.toList()]
    }

    private operator fun Any?.get(methodName: String, paramTypes: List<Class<*>>, params: List<Any?>): Any? {
        if (this == null) return null
        return this::class.java.getMethodOrNull(methodName, *paramTypes.toTypedArray())?.invoke(this, *params.toTypedArray())
    }

    private fun buildArtifact(
        executableName: String,
        linkTask: Task,
        runConfiguration: KonanRunConfigurationModel
    ): KonanArtifactModel? {
        val outputKind = linkTask["getOutputKind"]["name"] as? String ?: return null
        val konanTargetName = linkTask["getTarget"] as? String ?: error("No arch target found")
        val outputFile = (linkTask["getOutputFile"] as? Provider<*>)?.orNull as? File ?: return null
        val compilation = if (linkTask["getCompilation"] is Provider<*>)
            (linkTask["getCompilation"] as Provider<*>).get()
        else
            linkTask["getCompilation"]
        val compilationTarget = compilation["getTarget"]
        val compilationTargetName = compilationTarget["getName"] as? String ?: return null
        val isTests = linkTask["getProcessTests"] as? Boolean ?: return null

        return KonanArtifactModelImpl(
            compilationTargetName,
            executableName,
            outputKind,
            konanTargetName,
            outputFile,
            linkTask.path,
            runConfiguration,
            isTests
        )
    }

    private fun konanArtifacts(target: Named): List<KonanArtifactModel> {
        val result = ArrayList<KonanArtifactModel>()

        val binaries = target["getBinaries"] as? Collection<*> ?: return result
        binaries.forEach { binary ->
            val executableName = binary["getBaseName"] as? String ?: ""
            val linkTask = binary["getLinkTask"] as? Task ?: return@forEach
            val runConfiguration = KonanRunConfigurationModelImpl(binary["getRunTask"] as? Exec)
            buildArtifact(executableName, linkTask, runConfiguration)?.let { result.add(it) }
        }

        return result
    }

    private fun buildTarget(
        importingContext: MultiplatformModelImportingContext,
        target: KotlinTargetCompat,
        dependencyResolver: DependencyResolver,
        dependencyMapper: KotlinDependencyMapper
    ): KotlinTarget? {
        val platform = target.platform ?: return null
        val disambiguationClassifier = if (target.useDisambiguationClassifierAsSourceSetNamePrefix)
            target.disambiguationClassifier
        else
            target.overrideDisambiguationClassifierOnIdeImport

        val targetPresetName: String? = target.preset?.name

        val gradleCompilations = target.compilations ?: return null
        val compilations = gradleCompilations.mapNotNull {
            val compilation = buildCompilation(importingContext, it, disambiguationClassifier, dependencyResolver, dependencyMapper)
            if (compilation == null || platform != KotlinPlatform.ANDROID) {
                compilation
            } else {
                compilation.addDependsOnSourceSetsToCompilation(importingContext)
            }
        }
        val jar = buildTargetJar(gradleTarget, importingContext.project)
        val testRunTasks = buildTestRunTasks(importingContext.project, gradleTarget)
        val nativeMainRunTasks =
            if (platform == KotlinPlatform.NATIVE) buildNativeMainRunTasks(gradleTarget)
            else emptyList()
        val artifacts = konanArtifacts(gradleTarget)
        val target = KotlinTargetImpl(
            gradleTarget.name,
            targetPresetName,
            disambiguationClassifier,
            platform,
            compilations,
            testRunTasks,
            nativeMainRunTasks,
            jar,
            artifacts
        )
        compilations.forEach {
            it.disambiguationClassifier = target.disambiguationClassifier
            it.platform = target.platform
        }
        return target
    }

    private fun KotlinCompilationImpl.addDependsOnSourceSetsToCompilation(
        importingContext: MultiplatformModelImportingContext
    ): KotlinCompilationImpl {
        val dependsOnSourceSets = this.sourceSets.flatMap { it.dependsOnSourceSets }.mapNotNull { importingContext.sourceSetByName(it) }

        if (!importingContext.properties.isHmppEnabled) {
            // intermediate source sets should be common if HMPP is disabled
            dependsOnSourceSets.subtract(this.sourceSets).forEach {
                it.actualPlatforms.addSimplePlatforms(listOf(KotlinPlatform.COMMON))
            }
        }

        return KotlinCompilationImpl(
            this.name,
            this.sourceSets.union(dependsOnSourceSets),
            this.dependencies,
            this.output,
            this.arguments,
            this.dependencyClasspath,
            this.kotlinTaskProperties,
            this.nativeExtensions
        )
    }

    private fun buildNativeMainRunTasks(gradleTarget: Named): Collection<KotlinNativeMainRunTask> {
        val executableBinaries = (gradleTarget::class.java.getMethodOrNull("getBinaries")?.invoke(gradleTarget) as? Collection<Any>)
            ?.filter { it.javaClass.name == "org.jetbrains.kotlin.gradle.plugin.mpp.Executable" } ?: return emptyList()
        return executableBinaries.mapNotNull { binary ->
            val runTaskName = binary::class.java.getMethod("getRunTaskName").invoke(binary) as String? ?: return@mapNotNull null
            val entryPoint = binary::class.java.getMethod("getEntryPoint").invoke(binary) as String? ?: return@mapNotNull null
            val debuggable = binary::class.java.getMethod("getDebuggable").invoke(binary) as Boolean

            val compilationName = binary.javaClass.getMethodOrNull("getCompilation")?.invoke(binary)?.let {
                it.javaClass.getMethodOrNull("getCompilationName")?.invoke(it)?.toString()
            } ?: KotlinCompilation.MAIN_COMPILATION_NAME

            KotlinNativeMainRunTaskImpl(
                runTaskName,
                compilationName,
                entryPoint,
                debuggable
            )
        }
    }

    private fun Named.testTaskClass(className: String) = try {
        // This is a workaround that makes assumptions about the tasks naming logic
        // and is therefore an unstable and temporary solution until test runs API is implemented:
        @Suppress("UNCHECKED_CAST")
        this.javaClass.classLoader.loadClass(className) as Class<out Task>
    } catch (_: ClassNotFoundException) {
        null
    }

    private fun buildTestRunTasks(project: Project, gradleTarget: Named): Collection<KotlinTestRunTask> {
        val getTestRunsMethod = gradleTarget.javaClass.getMethodOrNull("getTestRuns")
        if (getTestRunsMethod != null) {
            val testRuns = getTestRunsMethod.invoke(gradleTarget) as? Iterable<Any>
            if (testRuns != null) {
                val testReports =
                    testRuns.mapNotNull { (it.javaClass.getMethodOrNull("getExecutionTask")?.invoke(it) as? TaskProvider<Task>)?.get() }
                val testTasks = testReports.flatMap {
                    ((it.javaClass.getMethodOrNull("getTestTasks")?.invoke(it) as? Collection<Any>)?.mapNotNull {
                        //TODO(auskov): getTestTasks should return collection of TaskProviders without mixing with Tasks
                        when (it) {
                            is Provider<*> -> it.get() as? Task
                            is Task -> it
                            else -> null
                        }
                    }) ?: listOf(it)
                }
                return testTasks.filter { it.enabled }.map {
                    val name = it.name
                    val compilation = it.javaClass.getMethodOrNull("getCompilation")?.invoke(it)
                    val compilationName = compilation?.javaClass?.getMethodOrNull("getCompilationName")?.invoke(compilation)?.toString()
                        ?: KotlinCompilation.TEST_COMPILATION_NAME
                    KotlinTestRunTaskImpl(name, compilationName)
                }.toList()
            }
            return emptyList()
        }

        // Otherwise, find the Kotlin/JVM test task with names matching the target name or
        // aggregate Android JVM tasks (like testDebugUnitTest).
        val kotlinTestTaskClass = gradleTarget.testTaskClass("org.jetbrains.kotlin.gradle.tasks.KotlinTest") ?: return emptyList()

        val targetDisambiguationClassifier = run {
            val getDisambiguationClassifier = gradleTarget.javaClass.getMethodOrNull("getDisambiguationClassifier")
                ?: return emptyList()

            getDisambiguationClassifier(gradleTarget) as String?
        }

        // The 'targetName' of a test task matches the target disambiguation classifier, potentially with suffix, e.g. jsBrowser
        val getTargetName = kotlinTestTaskClass.getDeclaredMethodOrNull("getTargetName") ?: return emptyList()

        val jvmTestTaskClass =
            gradleTarget.testTaskClass("org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest") ?: return emptyList()
        val getJvmTargetName = jvmTestTaskClass.getDeclaredMethodOrNull("getTargetName") ?: return emptyList()

        if (targetDisambiguationClassifier == "android") {
            val androidUnitTestClass = gradleTarget.testTaskClass("com.android.build.gradle.tasks.factory.AndroidUnitTest")
                ?: return emptyList()

            return project.tasks.filter { androidUnitTestClass.isInstance(it) }.mapNotNull { task -> task.name }
                .map { KotlinTestRunTaskImpl(it, KotlinCompilation.TEST_COMPILATION_NAME) }
        }

        return project.tasks.filter { kotlinTestTaskClass.isInstance(it) || jvmTestTaskClass.isInstance(it) }.mapNotNull { task ->
            val testTaskDisambiguationClassifier =
                (if (kotlinTestTaskClass.isInstance(task)) getTargetName(task) else getJvmTargetName(task)) as String?
            task.name.takeIf {
                targetDisambiguationClassifier.isNullOrEmpty() ||
                        testTaskDisambiguationClassifier != null &&
                        testTaskDisambiguationClassifier.startsWith(targetDisambiguationClassifier.orEmpty())
            }
        }.map { KotlinTestRunTaskImpl(it, KotlinCompilation.TEST_COMPILATION_NAME) }
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
        importingContext: MultiplatformModelImportingContext,
        compilation: KotlinCompilationCompat,
        classifier: String?,
        dependencyResolver: DependencyResolver,
        dependencyMapper: KotlinDependencyMapper
    ): KotlinCompilationImpl? {
        val compilationClass = gradleCompilation.javaClass
        val getKotlinSourceSets = compilationClass.getMethodOrNull("getKotlinSourceSets") ?: return null

        @Suppress("UNCHECKED_CAST")
        val kotlinGradleSourceSets = (getKotlinSourceSets(gradleCompilation) as? Collection<Named>) ?: return null
        val kotlinSourceSets = kotlinGradleSourceSets.mapNotNull { importingContext.sourceSetByName(it.name) }
        val compileKotlinTask = getCompileKotlinTaskName(importingContext.project, gradleCompilation) ?: return null
        val output = buildCompilationOutput(gradleCompilation, compileKotlinTask) ?: return null
        val arguments = buildCompilationArguments(compileKotlinTask)
        val dependencyClasspath = buildDependencyClasspath(compileKotlinTask)
        val dependencies =
            buildCompilationDependencies(importingContext, gradleCompilation, classifier, dependencyResolver, dependencyMapper)
        val kotlinTaskProperties = getKotlinTaskProperties(compileKotlinTask, classifier)

        // Get konanTarget (for native compilations only).
        val konanTarget = compilationClass.getMethodOrNull("getKonanTarget")?.let { getKonanTarget ->
            val konanTarget = getKonanTarget.invoke(gradleCompilation)
            konanTarget.javaClass.getMethodOrNull("getName")?.let {
                it.invoke(konanTarget) as? String
            }
        }
        val nativeExtensions = konanTarget?.let(::KotlinNativeCompilationExtensionsImpl)

        return KotlinCompilationImpl(
            gradleCompilation.name,
            kotlinSourceSets,
            dependencies.map { dependencyMapper.getId(it) }.distinct().toTypedArray(),
            output,
            arguments,
            dependencyClasspath.toTypedArray(),
            kotlinTaskProperties,
            nativeExtensions
        )
    }


    /**
     * Returns only those dependencies with RUNTIME scope which are not present with compile scope
     */
    private fun Collection<KotlinDependency>.onlyNewDependencies(compileDependencies: Collection<KotlinDependency>): List<KotlinDependency> {
        val compileDependencyArtefacts =
            compileDependencies.flatMap { (it as? ExternalProjectDependency)?.projectDependencyArtifacts ?: emptyList() }
        return this.filter {
            if (it is ExternalProjectDependency)
                !(compileDependencyArtefacts.containsAll(it.projectDependencyArtifacts))
            else
                true
        }
    }

    private fun buildCompilationDependencies(
        importingContext: MultiplatformModelImportingContext,
        gradleCompilation: Named,
        classifier: String?,
        dependencyResolver: DependencyResolver,
        dependencyMapper: KotlinDependencyMapper
    ): Set<KotlinDependency> {
        return LinkedHashSet<KotlinDependency>().apply {
            val transformationBuilder = MetadataDependencyTransformationBuilder(gradleCompilation)
            this += buildDependencies(
                gradleCompilation,
                dependencyResolver,
                "getCompileDependencyConfigurationName",
                "COMPILE",
                importingContext.project,
                transformationBuilder
            )
            this += buildDependencies(
                gradleCompilation,
                dependencyResolver,
                "getRuntimeDependencyConfigurationName",
                "RUNTIME",
                importingContext.project,
                transformationBuilder
            ).onlyNewDependencies(this)

            // TODO DISCUSS AT REVIEW: suspicious part
            val sourceSet = importingContext.sourceSetByName(compilationFullName(gradleCompilation.name, classifier))
            this += sourceSet?.dependencies?.mapNotNull { dependencyMapper.getDependency(it) } ?: emptySet()
        }
    }

    private class MetadataDependencyTransformationBuilder(val sourceSet: Any) {
        val transformationsMethod = sourceSet.javaClass.getMethodOrNull(, String::class.java)

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
        sourceSet: KotlinSourceSetCompat,
        dependencyResolver: DependencyResolver,
        importingContext: MultiplatformModelImportingContext
    ): List<KotlinDependency> {
        val project = importingContext.project

        fun String?.find(): Configuration? = this?.let { project.configurations.findByName(it) }

        return ArrayList<KotlinDependency>().apply {
            this += buildDependencies(
                sourceSet, dependencyResolver, "COMPILE", sourceSet.apiMetadataConfigurationName.find(), project
            )
            this += buildDependencies(
                sourceSet, dependencyResolver, "COMPILE", sourceSet.implementationMetadataConfigurationName.find(), project
            )
            this += buildDependencies(
                sourceSet, dependencyResolver, "COMPILE", sourceSet.compileOnlyMetadataConfigurationName.find(), project
            )
            this += buildDependencies(
                sourceSet, dependencyResolver, "RUNTIME", sourceSet.runtimeOnlyMetadataConfigurationName.find(), project
            ).onlyNewDependencies(this)

            this += buildAndroidSourceSetDependencies(sourceSet, importingContext.compatServices.androidDependencyResolver)
        }
    }

    private fun buildAndroidSourceSetDependencies(
        sourceSet: KotlinSourceSetCompat,
        androidDependencyResolverCompat: AndroidDependencyResolverCompat?
    ): Collection<KotlinDependency> {
        val androidDeps = androidDependencyResolverCompat?.androidSourceSetDependencies ?: return emptyList()

        return androidDeps[sourceSet.name]?.map { it ->
            @Suppress("UNCHECKED_CAST")
            val collection = it["getCollection"] as Set<File>?
            if (collection == null) {
                DefaultExternalLibraryDependency().apply {
                    (id as? DefaultExternalDependencyId)?.apply {
                        name = it["getName"] as String?
                        group = it["getGroup"] as String?
                        version = it["getVersion"] as String?
                    }
                    file = it["getJar"] as File
                    source = it["getSource"] as File?
                }
            } else {
                DefaultFileCollectionDependency(collection)
            }
        }.orEmpty()
    }

    @Suppress("UNCHECKED_CAST")
    private fun safelyGetArguments(compileKotlinTask: Task, accessor: Method?) = try {
        accessor?.invoke(compileKotlinTask) as? List<String>
    } catch (e: Exception) {
        logger.info(e.message ?: "Unexpected exception: $e", e)
        null
    } ?: emptyList()

    private fun buildCompilationArguments(compileKotlinTask: Task): KotlinCompilationArguments {
        val compileTaskClass = compileKotlinTask.javaClass
        val getCurrentArguments = compileTaskClass.getMethodOrNull("getSerializedCompilerArguments")
        val getDefaultArguments = compileTaskClass.getMethodOrNull("getDefaultSerializedCompilerArguments")
        val currentArguments = safelyGetArguments(compileKotlinTask, getCurrentArguments)
        val defaultArguments = safelyGetArguments(compileKotlinTask, getDefaultArguments)
        return KotlinCompilationArgumentsImpl(defaultArguments.toTypedArray(), currentArguments.toTypedArray())
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

    private fun computeSourceSetsDeferredInfo(importingContext: MultiplatformModelImportingContext) {
        for (sourceSet in importingContext.sourceSets) {
            if (!importingContext.properties.isHmppEnabled) {
                val name = sourceSet.name
                if (name == COMMON_MAIN_SOURCE_SET_NAME) {
                    sourceSet.isTestModule = false
                    continue
                }
                if (name == COMMON_TEST_SOURCE_SET_NAME) {
                    sourceSet.isTestModule = true
                    continue
                }
            }

            // TODO DISCUSS AT REVIEW: can we simplify it to:
            //      sourceSet.isTestModule = importingContext.compilationsBySourceSet(sourceSet)?.all { it.isTestModule} ?: false
            //                                                                                                           ^
            //                                                                                                           |
            //                                                                                                          sic!
            //  The difference from the current behaviour would be in case if source set is not included into any compilation AND
            //  KotlinSourceSetImpl.defaultIsTestModule is set to 'true'. Though I don't know who and why would do that.
            importingContext.compilationsBySourceSet(sourceSet)?.all { it.isTestModule }?.let { isTest ->
                sourceSet.isTestModule = isTest
            }

            importingContext.compilationsBySourceSet(sourceSet)?.let { compilations ->
                val platforms = compilations.map { it.platform }
                sourceSet.actualPlatforms.addSimplePlatforms(platforms)
            }

            if (sourceSet.shouldCoerceToCommon(importingContext.properties.isHmppEnabled, importingContext.properties.coerceRootSourceSetsToCommon)) {
                sourceSet.actualPlatforms.addSimplePlatforms(listOf(KotlinPlatform.COMMON))
            }
        }
    }

    private fun KotlinSourceSetImpl.shouldCoerceToCommon(isHMPPEnabled: Boolean, coerceRootSourceSetsToCommon: Boolean): Boolean {
        val isRoot = name == COMMON_MAIN_SOURCE_SET_NAME || name == COMMON_TEST_SOURCE_SET_NAME

        // never makes sense to coerce single-targeted source-sets
        if (actualPlatforms.platforms.size == 1) return false

        return when {
            // pre-HMPP has only single-targeted source sets and COMMON
            !isHMPPEnabled -> true

            // in HMPP, we might want to coerce source sets to common, but only root ones, and only
            // when the corresponding setting is turned on
            isHMPPEnabled && isRoot && coerceRootSourceSetsToCommon -> true

            // in all other cases, in HMPP we shouldn't coerce anything
            else -> false
        }
    }

    private class DependencyAdjuster(
        private val configuration: Configuration,
        private val scope: String,
        private val project: Project,
        transformations: Collection<KotlinMetadataDependencyTransformation>
    ) {
        private val adjustmentMap = HashMap<ExternalDependency, List<ExternalDependency>>()

        private val EXTRA_DEFAULT_CONFIGURATION_NAMES = listOf("metadataApiElements")

        private val projectDependencyTransformation =
            transformations.filter { it.projectPath != null }.associateBy { it.projectPath }

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
                if (dependency.configurationName != Dependency.DEFAULT_CONFIGURATION &&
                    !EXTRA_DEFAULT_CONFIGURATION_NAMES.contains(dependency.configurationName)
                )
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
                    listOf(wrapDependency(dependency, COMMON_MAIN_SOURCE_SET_NAME))
                }
                return if (platformDependency != null) listOf(platformDependency) + commonDependencies else commonDependencies
            }
        }
    }

    companion object {
        private val logger = Logging.getLogger(KotlinMPPGradleModelBuilder::class.java)
    }
}

private fun Project.getChildProjectByPath(path: String): Project? {
    var project = this
    for (name in path.split(":").asSequence().drop(1)) {
        project = project.childProjects[name] ?: return null
    }
    return project
}

