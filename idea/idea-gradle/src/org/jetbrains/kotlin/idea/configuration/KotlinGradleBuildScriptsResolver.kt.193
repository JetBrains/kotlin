/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Consumer
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters.*
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class KotlinGradleBuildScriptsResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinDslScriptsModel::class.java)
    }

    override fun getExtraJvmArgs(): List<Pair<String, String>> {
        return listOf(
            Pair(
                "-D$PROVIDER_MODE_SYSTEM_PROPERTY_NAME",
                STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE
            )
        )
    }

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        if (Registry.`is`("kotlin.gradle.scripts.useIdeaProjectImport", false)) {
            initScriptConsumer.consume("startParameter.taskNames += [\"${PREPARATION_TASK_NAME}\"]")
        }
    }

    override fun getModelProvider(): ProjectImportModelProvider {
        return KotlinDslScriptModelProvider()
    }

    override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
        super.populateProjectExtraModels(gradleProject, ideProject)

        resolverCtx.getExtraProject(null, KotlinDslScriptsModel::class.java)?.let { model ->
            // we need a copy to avoid memory leak, as model is java rmi proxy object
            ideProject.gradleKotlinBuildScripts = copy(model)
        }
    }

    override fun requiresTaskRunning() = true
}