/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.AsyncResult
import org.jdom.Element
import org.jetbrains.idea.maven.importing.MavenImporter
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.*
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.framework.libraryKind
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import java.io.File
import java.util.*

interface MavenProjectImportHandler {
    companion object : ProjectExtensionDescriptor<MavenProjectImportHandler>(
            "org.jetbrains.kotlin.mavenProjectImportHandler",
            MavenProjectImportHandler::class.java
    )

    operator fun invoke(facet: KotlinFacet, mavenProject: MavenProject)
}

class KotlinMavenImporter : MavenImporter(KOTLIN_PLUGIN_GROUP_ID, KOTLIN_PLUGIN_ARTIFACT_ID) {
    companion object {
        val KOTLIN_PLUGIN_GROUP_ID = "org.jetbrains.kotlin"
        val KOTLIN_PLUGIN_ARTIFACT_ID = "kotlin-maven-plugin"

        val KOTLIN_PLUGIN_SOURCE_DIRS_CONFIG = "sourceDirs"
    }

    override fun preProcess(module: Module, mavenProject: MavenProject, changes: MavenProjectChanges, modifiableModelsProvider: IdeModifiableModelsProvider) {
    }

    override fun process(modifiableModelsProvider: IdeModifiableModelsProvider,
                         module: Module,
                         rootModel: MavenRootModelAdapter,
                         mavenModel: MavenProjectsTree,
                         mavenProject: MavenProject,
                         changes: MavenProjectChanges,
                         mavenProjectToModuleName: MutableMap<MavenProject, String>,
                         postTasks: MutableList<MavenProjectsProcessorTask>) {

        if (changes.plugins) {
            contributeSourceDirectories(mavenProject, module, rootModel)
        }
    }

    override fun postProcess(module: Module, mavenProject: MavenProject, changes: MavenProjectChanges, modifiableModelsProvider: IdeModifiableModelsProvider) {
        super.postProcess(module, mavenProject, changes, modifiableModelsProvider)

        if (changes.dependencies) {
            scheduleDownloadStdlibSources(mavenProject, module)

            val targetLibraryKind = detectPlatformByExecutions(mavenProject)?.libraryKind
            if (targetLibraryKind != null) {
                modifiableModelsProvider.getModifiableRootModel(module).orderEntries().forEachLibrary { library ->
                    if ((library as LibraryEx).kind == null) {
                        val model = modifiableModelsProvider.getModifiableLibraryModel(library) as LibraryEx.ModifiableModelEx
                        detectLibraryKind(model.getFiles(OrderRootType.CLASSES))?.let { model.kind = it }
                    }
                    true
                }
            }
        }

        configureFacet(mavenProject, modifiableModelsProvider, module)
    }

    private fun scheduleDownloadStdlibSources(mavenProject: MavenProject, module: Module) {
        // TODO: here we have to process all kotlin libraries but for now we only handle standard libraries
        val artifacts = mavenProject.dependencyArtifactIndex.data[KOTLIN_PLUGIN_GROUP_ID]?.values?.flatMap { it.filter { it.isResolved } } ?: emptyList()

        val librariesWithNoSources = ArrayList<Library>()
        OrderEnumerator.orderEntries(module).forEachLibrary { library ->
            if (library.modifiableModel.getFiles(OrderRootType.SOURCES).isEmpty()) {
                librariesWithNoSources.add(library)
            }
            true
        }
        val libraryNames = librariesWithNoSources.mapTo(HashSet()) { it.name }
        val toBeDownloaded = artifacts.filter { it.libraryName in libraryNames }

        if (toBeDownloaded.isNotEmpty()) {
            MavenProjectsManager.getInstance(module.project).scheduleArtifactsDownloading(listOf(mavenProject), toBeDownloaded, true, false, AsyncResult())
        }
    }

    private fun getCompilerArgumentsByConfigurationElement(mavenProject: MavenProject,
                                                           configuration: Element?,
                                                           platform: TargetPlatformKind<*>): List<String> {
        val arguments = when (platform) {
            is TargetPlatformKind.Jvm -> K2JVMCompilerArguments()
            is TargetPlatformKind.JavaScript -> K2JSCompilerArguments()
            is TargetPlatformKind.Common -> K2MetadataCompilerArguments()
        }

        arguments.apiVersion = configuration?.getChild("apiVersion")?.text ?: mavenProject.properties["kotlin.compiler.apiVersion"]?.toString()
        arguments.languageVersion = configuration?.getChild("languageVersion")?.text ?: mavenProject.properties["kotlin.compiler.languageVersion"]?.toString()
        arguments.multiPlatform = configuration?.getChild("multiPlatform")?.text?.trim()?.toBoolean() ?: false
        arguments.suppressWarnings = configuration?.getChild("nowarn")?.text?.trim()?.toBoolean() ?: false

        configuration?.getChild("experimentalCoroutines")?.text?.trim()?.let {
            arguments.coroutinesState = it
        }

        when (arguments) {
            is K2JVMCompilerArguments -> {
                arguments.classpath = configuration?.getChild("classpath")?.text
                arguments.jdkHome = configuration?.getChild("jdkHome")?.text
                arguments.jvmTarget = configuration?.getChild("jvmTarget")?.text ?: mavenProject.properties["kotlin.compiler.jvmTarget"]?.toString()
            }
            is K2JSCompilerArguments -> {
                arguments.sourceMap = configuration?.getChild("sourceMap")?.text?.trim()?.toBoolean() ?: false
                arguments.sourceMapPrefix = configuration?.getChild("sourceMapPrefix")?.text?.trim() ?: ""
                arguments.sourceMapEmbedSources = configuration?.getChild("sourceMapEmbedSources")?.text?.trim() ?: "inlining"
                arguments.outputFile = configuration?.getChild("outputFile")?.text
                arguments.metaInfo = configuration?.getChild("metaInfo")?.text?.trim()?.toBoolean() ?: false
                arguments.moduleKind = configuration?.getChild("moduleKind")?.text
                arguments.main = configuration?.getChild("main")?.text
            }
        }

        val additionalArgs = configuration?.getChild("args")?.getChildren("arg")?.mapNotNull { it.text }.orEmpty()
        parseCommandLineArguments(additionalArgs, arguments)

        return ArgumentUtils.convertArgumentsToStringList(arguments)
    }

    private val compilationGoals = listOf(PomFile.KotlinGoals.Compile,
                                          PomFile.KotlinGoals.TestCompile,
                                          PomFile.KotlinGoals.Js,
                                          PomFile.KotlinGoals.TestJs,
                                          PomFile.KotlinGoals.MetaData)

    private fun configureFacet(mavenProject: MavenProject, modifiableModelsProvider: IdeModifiableModelsProvider, module: Module) {
        val mavenPlugin = mavenProject.findPlugin(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID) ?: return
        val compilerVersion = mavenPlugin.version ?: LanguageVersion.LATEST_STABLE.versionString
        val kotlinFacet = module.getOrCreateFacet(modifiableModelsProvider, false)
        val platform = detectPlatformByExecutions(mavenProject) ?: detectPlatformByLibraries(mavenProject)

        kotlinFacet.configureFacet(compilerVersion, LanguageFeature.Coroutines.defaultState, platform, modifiableModelsProvider)
        val configuredPlatform = kotlinFacet.configuration.settings.targetPlatformKind!!
        val configuration = mavenPlugin.configurationElement
        val sharedArguments = getCompilerArgumentsByConfigurationElement(mavenProject, configuration, configuredPlatform)
        val executionArguments = mavenPlugin.executions?.filter { it.goals.any { it in compilationGoals } }
                                         ?.firstOrNull()
                                         ?.configurationElement?.let { getCompilerArgumentsByConfigurationElement(mavenProject, it, configuredPlatform) }
        parseCompilerArgumentsToFacet(sharedArguments, emptyList(), kotlinFacet, modifiableModelsProvider)
        if (executionArguments != null) {
            parseCompilerArgumentsToFacet(executionArguments, emptyList(), kotlinFacet, modifiableModelsProvider)
        }
        MavenProjectImportHandler.getInstances(module.project).forEach { it(kotlinFacet, mavenProject) }
    }

    private fun detectPlatformByExecutions(mavenProject: MavenProject): TargetPlatformKind<*>? {
        return mavenProject.findPlugin(KOTLIN_PLUGIN_GROUP_ID, KOTLIN_PLUGIN_ARTIFACT_ID)?.executions?.flatMap { it.goals }?.mapNotNull { goal ->
            when (goal) {
                PomFile.KotlinGoals.Compile, PomFile.KotlinGoals.TestCompile -> TargetPlatformKind.Jvm[JvmTarget.JVM_1_6]
                PomFile.KotlinGoals.Js, PomFile.KotlinGoals.TestJs -> TargetPlatformKind.JavaScript
                PomFile.KotlinGoals.MetaData -> TargetPlatformKind.Common
                else -> null
            }
        }?.distinct()?.singleOrNull()
    }

    private fun detectPlatformByLibraries(mavenProject: MavenProject): TargetPlatformKind<*>? {
        return TargetPlatformKind.ALL_PLATFORMS.firstOrNull {
            it.mavenLibraryIds.any { mavenProject.findDependencies(KOTLIN_PLUGIN_GROUP_ID, it).isNotEmpty() }
        }
    }

    // TODO in theory it should work like this but it doesn't as it couldn't unmark source roots that are not roots anymore.
    //     I believe this is something should be done by the underlying maven importer implementation or somewhere else in the IDEA
    //     For now there is a contributeSourceDirectories implementation that deals with the issue
    //        see https://youtrack.jetbrains.com/issue/IDEA-148280

    //    override fun collectSourceRoots(mavenProject: MavenProject, result: PairConsumer<String, JpsModuleSourceRootType<*>>) {
    //        for ((type, dir) in collectSourceDirectories(mavenProject)) {
    //            val jpsType: JpsModuleSourceRootType<*> = when (type) {
    //                SourceType.PROD -> JavaSourceRootType.SOURCE
    //                SourceType.TEST -> JavaSourceRootType.TEST_SOURCE
    //            }
    //
    //            result.consume(dir, jpsType)
    //        }
    //    }

    private fun contributeSourceDirectories(mavenProject: MavenProject, module: Module, rootModel: MavenRootModelAdapter) {
        val directories = collectSourceDirectories(mavenProject)

        val toBeAdded = directories.map { it.second }.toSet()
        val state = module.kotlinImporterComponent

        for ((type, dir) in directories) {
            if (rootModel.getSourceFolder(File(dir)) == null) {
                val jpsType: JpsModuleSourceRootType<*> = when (type) {
                    SourceType.TEST -> JavaSourceRootType.TEST_SOURCE
                    SourceType.PROD -> JavaSourceRootType.SOURCE
                }

                rootModel.addSourceFolder(dir, jpsType)
            }
        }

        state.addedSources.filter { it !in toBeAdded }.forEach {
            rootModel.unregisterAll(it, true, true)
            state.addedSources.remove(it)
        }
        state.addedSources.addAll(toBeAdded)
    }

    private fun collectSourceDirectories(mavenProject: MavenProject): List<Pair<SourceType, String>> =
            mavenProject.plugins.filter { it.isKotlinPlugin() }.flatMap { plugin ->
                plugin.configurationElement.sourceDirectories().map { SourceType.PROD to it } +
                plugin.executions.flatMap { execution -> execution.configurationElement.sourceDirectories().map { execution.sourceType() to it } }
            }.distinct()
}

private fun MavenPlugin.isKotlinPlugin() = groupId == KotlinMavenImporter.KOTLIN_PLUGIN_GROUP_ID && artifactId == KotlinMavenImporter.KOTLIN_PLUGIN_ARTIFACT_ID
private fun Element?.sourceDirectories(): List<String> = this?.getChildren(KotlinMavenImporter.KOTLIN_PLUGIN_SOURCE_DIRS_CONFIG)?.flatMap { it.children ?: emptyList() }?.map { it.textTrim } ?: emptyList()
private fun MavenPlugin.Execution.sourceType() =
        goals.map { if (isTestGoalName(it)) SourceType.TEST else SourceType.PROD }
                .distinct()
                .singleOrNull() ?: SourceType.PROD

private fun isTestGoalName(goalName: String) = goalName.startsWith("test-")

private enum class SourceType {
    PROD, TEST
}

@State(name = "AutoImportedSourceRoots",
       storages = arrayOf(
               Storage(id = "other", file = StoragePathMacros.MODULE_FILE)
       ))
class KotlinImporterComponent : PersistentStateComponent<KotlinImporterComponent.State> {
    class State(var directories: List<String> = ArrayList())

    val addedSources = Collections.synchronizedSet(HashSet<String>())

    override fun loadState(state: State?) {
        addedSources.clear()
        if (state != null) {
            addedSources.addAll(state.directories)
        }
    }

    override fun getState(): State {
        return State(addedSources.sorted())
    }
}

private val Module.kotlinImporterComponent: KotlinImporterComponent
    get() = getComponent(KotlinImporterComponent::class.java) ?: throw IllegalStateException("No maven importer state configured")
