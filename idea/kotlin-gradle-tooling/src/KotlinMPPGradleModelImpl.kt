/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.tasks.Exec
import java.io.File

class KotlinSourceSetProto(
    val name: String,
    private val languageSettings: KotlinLanguageSettings,
    private val sourceDirs: Set<File>,
    private val resourceDirs: Set<File>,
    private val dependencies: () -> Array<KotlinDependencyId>,
    val dependsOnSourceSets: Set<String>
) {
    fun buildKotlinSourceSetImpl(
        doBuildDependencies: Boolean,
        allSourceSetsProtosByNames: Map<String, KotlinSourceSetProto>,
    ) = KotlinSourceSetImpl(
        name = name,
        languageSettings = languageSettings,
        sourceDirs = sourceDirs,
        resourceDirs = resourceDirs,
        dependencies = if (doBuildDependencies) dependencies.invoke() else emptyArray(),
        declaredDependsOnSourceSets = dependsOnSourceSets,
        allDependsOnSourceSets = allDependsOnSourceSets(allSourceSetsProtosByNames)
    )
}

fun KotlinSourceSetProto.allDependsOnSourceSets(sourceSetsByName: Map<String, KotlinSourceSetProto>): Set<String> {
    return mutableSetOf<String>().apply {
        addAll(dependsOnSourceSets)
        dependsOnSourceSets.map(sourceSetsByName::getValue).forEach { dependsOnSourceSet ->
            addAll(dependsOnSourceSet.allDependsOnSourceSets(sourceSetsByName))
        }
    }
}

class KotlinSourceSetImpl(
    override val name: String,
    override val languageSettings: KotlinLanguageSettings,
    override val sourceDirs: Set<File>,
    override val resourceDirs: Set<File>,
    override val dependencies: Array<KotlinDependencyId>,
    override val declaredDependsOnSourceSets: Set<String>,
    @Suppress("OverridingDeprecatedMember")
    override val allDependsOnSourceSets: Set<String>,
    defaultActualPlatforms: KotlinPlatformContainerImpl = KotlinPlatformContainerImpl(),
    defaultIsTestModule: Boolean = false
) : KotlinSourceSet {

    @Suppress("DEPRECATION")
    constructor(kotlinSourceSet: KotlinSourceSet) : this(
        name = kotlinSourceSet.name,
        languageSettings = KotlinLanguageSettingsImpl(kotlinSourceSet.languageSettings),
        sourceDirs = HashSet(kotlinSourceSet.sourceDirs),
        resourceDirs = HashSet(kotlinSourceSet.resourceDirs),
        dependencies = kotlinSourceSet.dependencies.clone(),
        declaredDependsOnSourceSets = HashSet(kotlinSourceSet.declaredDependsOnSourceSets),
        allDependsOnSourceSets = HashSet(kotlinSourceSet.allDependsOnSourceSets),
        defaultActualPlatforms = KotlinPlatformContainerImpl(kotlinSourceSet.actualPlatforms)
    ) {
        this.isTestModule = kotlinSourceSet.isTestModule
    }

    override var actualPlatforms: KotlinPlatformContainer = defaultActualPlatforms
        internal set

    override var isTestModule: Boolean = defaultIsTestModule
        internal set

    override fun toString() = name

    init {
        @Suppress("DEPRECATION")
        require(allDependsOnSourceSets.containsAll(declaredDependsOnSourceSets)) {
            "Inconsistent source set dependencies: 'allDependsOnSourceSets' is expected to contain all 'declaredDependsOnSourceSets'"
        }
    }
}

data class KotlinLanguageSettingsImpl(
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val isProgressiveMode: Boolean,
    override val enabledLanguageFeatures: Set<String>,
    override val experimentalAnnotationsInUse: Set<String>,
    override val compilerPluginArguments: Array<String>,
    override val compilerPluginClasspath: Set<File>,
    override val freeCompilerArgs: Array<String>
) : KotlinLanguageSettings {
    constructor(settings: KotlinLanguageSettings) : this(
        settings.languageVersion,
        settings.apiVersion,
        settings.isProgressiveMode,
        settings.enabledLanguageFeatures,
        settings.experimentalAnnotationsInUse,
        settings.compilerPluginArguments,
        settings.compilerPluginClasspath,
        settings.freeCompilerArgs
    )
}

data class KotlinCompilationOutputImpl(
    override val classesDirs: Set<File>,
    override val effectiveClassesDir: File?,
    override val resourcesDir: File?
) : KotlinCompilationOutput {
    constructor(output: KotlinCompilationOutput) : this(
        HashSet(output.classesDirs),
        output.effectiveClassesDir,
        output.resourcesDir
    )
}

data class KotlinCompilationArgumentsImpl(
    override val defaultArguments: Array<String>,
    override val currentArguments: Array<String>
) : KotlinCompilationArguments {
    constructor(arguments: KotlinCompilationArguments) : this(
        arguments.defaultArguments,
        arguments.currentArguments
    )
}

data class KotlinNativeCompilationExtensionsImpl(
    override val konanTarget: String
) : KotlinNativeCompilationExtensions {
    constructor(extensions: KotlinNativeCompilationExtensions) : this(extensions.konanTarget)
}

data class KotlinCompilationImpl(
    override val name: String,
    override val allSourceSets: Collection<KotlinSourceSet>,
    override val declaredSourceSets: Collection<KotlinSourceSet>,
    override val dependencies: Array<KotlinDependencyId>,
    override val output: KotlinCompilationOutput,
    override val arguments: KotlinCompilationArguments,
    override val dependencyClasspath: Array<String>,
    override val kotlinTaskProperties: KotlinTaskProperties,
    override val nativeExtensions: KotlinNativeCompilationExtensions?
) : KotlinCompilation {

    // create deep copy
    constructor(kotlinCompilation: KotlinCompilation, cloningCache: MutableMap<Any, Any>) : this(
        name = kotlinCompilation.name,
        declaredSourceSets = cloneSourceSetsWithCaching(kotlinCompilation.declaredSourceSets, cloningCache),
        allSourceSets = cloneSourceSetsWithCaching(kotlinCompilation.allSourceSets, cloningCache),
        dependencies = kotlinCompilation.dependencies,
        output = KotlinCompilationOutputImpl(kotlinCompilation.output),
        arguments = KotlinCompilationArgumentsImpl(kotlinCompilation.arguments),
        dependencyClasspath = kotlinCompilation.dependencyClasspath,
        kotlinTaskProperties = KotlinTaskPropertiesImpl(kotlinCompilation.kotlinTaskProperties),
        nativeExtensions = kotlinCompilation.nativeExtensions?.let(::KotlinNativeCompilationExtensionsImpl)
    ) {
        disambiguationClassifier = kotlinCompilation.disambiguationClassifier
        platform = kotlinCompilation.platform
    }

    override var disambiguationClassifier: String? = null
        internal set
    override lateinit var platform: KotlinPlatform
        internal set

    // TODO: Logic like this is duplicated *and different*
    override val isTestModule: Boolean
        get() = name == KotlinCompilation.TEST_COMPILATION_NAME
                || platform == KotlinPlatform.ANDROID && name.contains("Test")

    override fun toString() = name

    companion object {
        private fun cloneSourceSetsWithCaching(
            sourceSets: Collection<KotlinSourceSet>,
            cloningCache: MutableMap<Any, Any>
        ): List<KotlinSourceSet> =
            sourceSets.map { initialSourceSet ->
                (cloningCache[initialSourceSet] as? KotlinSourceSet) ?: KotlinSourceSetImpl(initialSourceSet).also {
                    cloningCache[initialSourceSet] = it
                }
            }

    }
}

data class KotlinTargetJarImpl(
    override val archiveFile: File?
) : KotlinTargetJar

data class KotlinTargetImpl(
    override val name: String,
    override val presetName: String?,
    override val disambiguationClassifier: String?,
    override val platform: KotlinPlatform,
    override val compilations: Collection<KotlinCompilation>,
    override val testRunTasks: Collection<KotlinTestRunTask>,
    override val nativeMainRunTasks: Collection<KotlinNativeMainRunTask>,
    override val jar: KotlinTargetJar?,
    override val konanArtifacts: List<KonanArtifactModel>
) : KotlinTarget {
    override fun toString() = name

    constructor(target: KotlinTarget, cloningCache: MutableMap<Any, Any>) : this(
        target.name,
        target.presetName,
        target.disambiguationClassifier,
        KotlinPlatform.byId(target.platform.id) ?: KotlinPlatform.COMMON,
        target.compilations.map { initialCompilation ->
            (cloningCache[initialCompilation] as? KotlinCompilation) ?: KotlinCompilationImpl(initialCompilation, cloningCache).also {
                cloningCache[initialCompilation] = it
            }
        }.toList(),
        target.testRunTasks.map { initialTestTask ->
            (cloningCache[initialTestTask] as? KotlinTestRunTask)
                ?: KotlinTestRunTaskImpl(
                    initialTestTask.taskName,
                    initialTestTask.compilationName
                ).also {
                    cloningCache[initialTestTask] = it
                }
        },
        target.nativeMainRunTasks.map { initialTestTask ->
            (cloningCache[initialTestTask] as? KotlinNativeMainRunTask)
                ?: KotlinNativeMainRunTaskImpl(
                    initialTestTask.taskName,
                    initialTestTask.compilationName,
                    initialTestTask.entryPoint,
                    initialTestTask.debuggable
                ).also {
                    cloningCache[initialTestTask] = it
                }
        },
        KotlinTargetJarImpl(target.jar?.archiveFile),
        target.konanArtifacts.map { KonanArtifactModelImpl(it) }.toList()
    )
}

data class KotlinTestRunTaskImpl(
    override val taskName: String,
    override val compilationName: String
) : KotlinTestRunTask

data class KotlinNativeMainRunTaskImpl(
    override val taskName: String,
    override val compilationName: String,
    override val entryPoint: String,
    override val debuggable: Boolean
) : KotlinNativeMainRunTask

data class ExtraFeaturesImpl(
    override val coroutinesState: String?,
    override val isHMPPEnabled: Boolean,
    override val isNativeDependencyPropagationEnabled: Boolean
) : ExtraFeatures

data class KotlinMPPGradleModelImpl(
    override val sourceSetsByName: Map<String, KotlinSourceSet>,
    override val targets: Collection<KotlinTarget>,
    override val extraFeatures: ExtraFeatures,
    override val kotlinNativeHome: String,
    override val dependencyMap: Map<KotlinDependencyId, KotlinDependency>,
    override val kotlinImportingDiagnostics: KotlinImportingDiagnosticsContainer = mutableSetOf()
) : KotlinMPPGradleModel {

    constructor(mppModel: KotlinMPPGradleModel, cloningCache: MutableMap<Any, Any>) : this(
        mppModel.sourceSetsByName.mapValues { initialSourceSet ->
            (cloningCache[initialSourceSet] as? KotlinSourceSet) ?: KotlinSourceSetImpl(initialSourceSet.value)
                .also { cloningCache[initialSourceSet] = it }
        },
        mppModel.targets.map { initialTarget ->
            (cloningCache[initialTarget] as? KotlinTarget) ?: KotlinTargetImpl(initialTarget, cloningCache).also {
                cloningCache[initialTarget] = it
            }
        }.toList(),
        ExtraFeaturesImpl(
            mppModel.extraFeatures.coroutinesState,
            mppModel.extraFeatures.isHMPPEnabled,
            mppModel.extraFeatures.isNativeDependencyPropagationEnabled
        ),
        mppModel.kotlinNativeHome,
        mppModel.dependencyMap.map { it.key to it.value.deepCopy(cloningCache) }.toMap(),
        mppModel.kotlinImportingDiagnostics.mapTo(mutableSetOf()) { it.deepCopy(cloningCache) }
    )
}

class KotlinPlatformContainerImpl() : KotlinPlatformContainer {
    private val defaultCommonPlatform = setOf(KotlinPlatform.COMMON)
    private var myPlatforms: MutableSet<KotlinPlatform>? = null

    override val arePlatformsInitialized: Boolean
        get() = myPlatforms != null

    constructor(platform: KotlinPlatformContainer) : this() {
        myPlatforms = HashSet(platform.platforms)
    }

    override val platforms: Set<KotlinPlatform>
        get() = myPlatforms ?: defaultCommonPlatform

    override fun supports(simplePlatform: KotlinPlatform): Boolean = platforms.contains(simplePlatform)

    override fun pushPlatforms(platforms: Iterable<KotlinPlatform>) {
        myPlatforms = (myPlatforms ?: LinkedHashSet()).apply {
            addAll(platforms)
            if (contains(KotlinPlatform.COMMON)) {
                clear()
                addAll(defaultCommonPlatform)
            }
        }
    }
}

data class KonanArtifactModelImpl(
    override val targetName: String,
    override val executableName: String,
    override val type: String,
    override val targetPlatform: String,
    override val file: File,
    override val buildTaskPath: String,
    override val runConfiguration: KonanRunConfigurationModel,
    override val isTests: Boolean
) : KonanArtifactModel {
    constructor(artifact: KonanArtifactModel) : this(
        artifact.targetName,
        artifact.executableName,
        artifact.type,
        artifact.targetPlatform,
        artifact.file,
        artifact.buildTaskPath,
        KonanRunConfigurationModelImpl(artifact.runConfiguration),
        artifact.isTests
    )
}

data class KonanRunConfigurationModelImpl(
    override val workingDirectory: String,
    override val programParameters: List<String>,
    override val environmentVariables: Map<String, String>
) : KonanRunConfigurationModel {
    constructor(configuration: KonanRunConfigurationModel) : this(
        configuration.workingDirectory,
        ArrayList(configuration.programParameters),
        HashMap(configuration.environmentVariables)
    )

    constructor(runTask: Exec?) : this(
        runTask?.workingDir?.path ?: KonanRunConfigurationModel.NO_WORKING_DIRECTORY,
        runTask?.args as List<String>? ?: KonanRunConfigurationModel.NO_PROGRAM_PARAMETERS,
        (runTask?.environment as Map<String, Any>?)
            ?.mapValues { it.value.toString() } ?: KonanRunConfigurationModel.NO_ENVIRONMENT_VARIABLES
    )
}
