/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.gradle.KotlinDslScriptAdditionalTask
import org.jetbrains.kotlin.gradle.KotlinDslScriptModelProvider
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported
import org.jetbrains.plugins.gradle.model.Build
import org.jetbrains.plugins.gradle.model.ClassSetImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider

class KotlinDslScriptModelResolver : KotlinDslScriptModelResolverCommon() {
    override fun requiresTaskRunning() = true

    override fun getModelProvider() = KotlinDslScriptModelProvider()

    override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider? {
        return ClassSetImportModelProvider(
            emptySet(),
            setOf(KotlinDslScriptAdditionalTask::class.java)
        )
    }

    override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
        super.populateProjectExtraModels(gradleProject, ideProject)

        if (kotlinDslScriptsModelImportSupported(resolverCtx.projectGradleVersion)) {
            populateBuildModels(resolverCtx.models.mainBuild, ideProject)

            resolverCtx.models.includedBuilds.forEach { includedRoot ->
                populateBuildModels(includedRoot, ideProject)
            }
        }
    }

    private fun populateBuildModels(
        root: Build,
        ideProject: DataNode<ProjectData>
    ) {
        root.projects.forEach {
            if (it.projectIdentifier.projectPath == ":") {
                resolverCtx.models.getModel(it, KotlinDslScriptsModel::class.java)?.let { model ->
                    processScriptModel(resolverCtx, model, it.projectIdentifier.projectPath)
                }
            }
        }
    }
}