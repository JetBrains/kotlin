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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import org.jdom.Element
import org.jetbrains.idea.maven.importing.MavenImporter
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectChanges
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask
import org.jetbrains.idea.maven.project.MavenProjectsTree
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import java.io.File
import java.util.*

private val KotlinPluginGroupId = "org.jetbrains.kotlin"
private val KotlinPluginArtifactId = "kotlin-maven-plugin"
private val KotlinPluginSourceDirsConfig = "sourceDirs"

class KotlinMavenImporter : MavenImporter(KotlinPluginGroupId, KotlinPluginArtifactId) {
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

private fun MavenPlugin.isKotlinPlugin() = groupId == KotlinPluginGroupId && artifactId == KotlinPluginArtifactId
private fun Element?.sourceDirectories(): List<String> = this?.getChildren(KotlinPluginSourceDirsConfig)?.flatMap { it.children ?: emptyList() }?.map { it.textTrim } ?: emptyList()
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

    override fun loadState(state: KotlinImporterComponent.State?) {
        addedSources.clear()
        if (state != null) {
            addedSources.addAll(state.directories)
        }
    }

    override fun getState(): KotlinImporterComponent.State {
        return KotlinImporterComponent.State(addedSources.sorted())
    }
}

private val Module.kotlinImporterComponent: KotlinImporterComponent
    get() = getComponent(KotlinImporterComponent::class.java) ?: throw IllegalStateException("No maven importer state configured")