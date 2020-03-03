/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.cocoapods

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.model.ClassSetProjectImportModelProvider
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext

internal const val POD_IMPORT_TASK_NAME = "podImport"

class KotlinCocoaPodsModelResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinCocoaPodsModel::class.java)
    }

    override fun getProjectsLoadedModelProvider(): ProjectImportModelProvider {
        return ClassSetProjectImportModelProvider(
            setOf(KotlinCocoaPodsModel::class.java)
        )
    }
}

interface KotlinCocoaPodsModel

class KotlinCocoaPodsModelBuilder : AbstractModelBuilderService() {
    override fun canBuild(modelName: String?): Boolean {
        return KotlinCocoaPodsModel::class.java.name == modelName
    }

    override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
        val startParameter = project.gradle.startParameter
        val taskNames = startParameter.taskNames

        if (canBuild(modelName) && project.tasks.findByPath(POD_IMPORT_TASK_NAME) != null && POD_IMPORT_TASK_NAME !in taskNames) {
            taskNames.add(POD_IMPORT_TASK_NAME)
            startParameter.setTaskNames(taskNames)
        }
        return null
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(
            project, e, "Kotlin CocoaPods model error"
        ).withDescription("Unable to create ${POD_IMPORT_TASK_NAME} task.")
    }
}