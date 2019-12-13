/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import org.gradle.tooling.BuildController
import org.gradle.tooling.model.Model
import org.gradle.tooling.model.gradle.GradleBuild
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider

class KotlinDslScriptModelProvider : ProjectImportModelProvider {
    private val kotlinDslScriptModelClass: Class<*> = KotlinDslScriptsModel::class.java

    override fun populateBuildModels(
        controller: BuildController,
        buildModel: GradleBuild,
        consumer: ProjectImportModelProvider.BuildModelConsumer
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
        projectModel: Model,
        modelConsumer: ProjectImportModelProvider.ProjectModelConsumer
    ) = Unit
}