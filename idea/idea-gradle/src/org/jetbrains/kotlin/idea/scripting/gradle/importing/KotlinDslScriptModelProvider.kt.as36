/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import org.gradle.tooling.BuildController
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.plugins.gradle.model.ProjectImportExtraModelProvider

class KotlinDslScriptModelProvider : ProjectImportExtraModelProvider {
    private val kotlinDslScriptModelClass: Class<*> = KotlinDslScriptsModel::class.java

    override fun populateBuildModels(
        controller: BuildController,
        project: IdeaProject,
        consumer: ProjectImportExtraModelProvider.BuildModelConsumer
    ) {
        project.modules.forEach { module ->
            if (module.gradleProject.parent == null) {
                val model = controller.findModel(module.gradleProject, kotlinDslScriptModelClass)
                if (model != null) {
                    consumer.consume(module, model, kotlinDslScriptModelClass)
                }
            }
        }
    }

    override fun populateProjectModels(
        controller: BuildController,
        module: IdeaModule?,
        modelConsumer: ProjectImportExtraModelProvider.ProjectModelConsumer
    ) = Unit
}