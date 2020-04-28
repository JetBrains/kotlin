/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.jetbrains.plugins.gradle.model.ClassSetImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.kotlin.gradle.KotlinDslScriptAdditionalTask
import org.jetbrains.kotlin.gradle.KotlinDslScriptModelProvider
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported
import org.jetbrains.plugins.gradle.service.project.ModifiableGradleProjectModel
import org.jetbrains.plugins.gradle.service.project.ProjectModelContributor
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.service.project.ToolingModelsProvider

class KotlinDslScriptModelResolver : KotlinDslScriptModelResolverCommon() {
    override fun requiresTaskRunning() = true

    override fun getModelProvider() = KotlinDslScriptModelProvider()

    override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider? {
        return ClassSetImportModelProvider(
            emptySet(),
            setOf(KotlinDslScriptAdditionalTask::class.java)
        )
    }
}

@Suppress("UnstableApiUsage")
class KotlinDslScriptModelContributor : ProjectModelContributor {
    override fun accept(
        projectModelBuilder: ModifiableGradleProjectModel,
        toolingModelsProvider: ToolingModelsProvider,
        resolverContext: ProjectResolverContext
    ) {
        if (!kotlinDslScriptsModelImportSupported(resolverContext.projectGradleVersion)) return

        toolingModelsProvider.projects().forEach {
            val projectIdentifier = it.projectIdentifier.projectPath
            if (projectIdentifier == ":") {
                val model = toolingModelsProvider.getProjectModel(it, KotlinDslScriptsModel::class.java)
                if (model != null) {
                    processScriptModel(resolverContext, model, projectIdentifier)
                }
            }
        }
    }
}