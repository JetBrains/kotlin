/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported

class KotlinDslScriptModelResolver : KotlinDslScriptModelResolverCommon() {
    override fun getModelProvider() = KotlinDslScriptModelProvider()
    override fun requiresTaskRunning() = true

    override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
        super.populateProjectExtraModels(gradleProject, ideProject)

        if (kotlinDslScriptsModelImportSupported(resolverCtx.projectGradleVersion)) {
            populateBuildModels(gradleProject, ideProject)

            resolverCtx.models.includedBuilds.forEach { includedRoot ->
                populateBuildModels(includedRoot, ideProject)
            }
        }
    }

    private fun populateBuildModels(
        root: IdeaProject,
        ideProject: DataNode<ProjectData>
    ) {
        root.modules.forEach {
            if (it.gradleProject.parent == null) {
                resolverCtx.getExtraProject(it, KotlinDslScriptsModel::class.java)?.let { model ->
                    ideProject.KOTLIN_DSL_SCRIPT_MODELS.addAll(model.toListOfScriptModels())
                }
            }
        }
    }
}