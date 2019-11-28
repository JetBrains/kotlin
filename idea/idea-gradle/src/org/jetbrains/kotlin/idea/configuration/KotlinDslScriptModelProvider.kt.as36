/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

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
        val model = controller.findModel(kotlinDslScriptModelClass)
        if (model != null) {
            consumer.consume(null, model, kotlinDslScriptModelClass)
        }
    }

    override fun populateProjectModels(
        controller: BuildController,
        module: IdeaModule?,
        modelConsumer: ProjectImportExtraModelProvider.ProjectModelConsumer
    ) = Unit
}