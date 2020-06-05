/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext


interface EnableCommonizerTask

class KotlinCommonizerModelBuilder : AbstractModelBuilderService() {

    companion object {
        private const val COMMONIZER_TASK_NAME = "runCommonizer"
        private const val COMMONIZER_SETUP_CLASS = "org.jetbrains.kotlin.gradle.targets.native.internal.KotlinNativePlatformDependenciesKt"
    }

    override fun canBuild(modelName: String?): Boolean {
        return EnableCommonizerTask::class.java.name == modelName
    }

    override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null

        try {
            val classLoader = kotlinExt.javaClass.classLoader
            val clazz = try {
                Class.forName(COMMONIZER_SETUP_CLASS, false, classLoader)
            } catch (e: ClassNotFoundException) {
                //It can be old version mpp gradle plugin. Supported only 1.4+
                return null
            }
            val isAllowCommonizerFun = clazz.getMethodOrNull("isAllowCommonizer", Project::class.java) ?: return null

            val isAllowCommonizer = isAllowCommonizerFun.invoke(Boolean::class.java, project) as Boolean

            if (isAllowCommonizer) {
                val startParameter = project.gradle.startParameter
                val tasks = HashSet(startParameter.taskNames)
                if (!tasks.contains(COMMONIZER_TASK_NAME)) {
                    tasks.add(COMMONIZER_TASK_NAME)
                    startParameter.setTaskNames(tasks)
                }
            }
        } catch (e: Exception) {
            project.logger.error(
                getErrorMessageBuilder(project, e).build()
            )
        }
        return null
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(
            project, e, "EnableCommonizerTask error"
        ).withDescription("Unable to create $COMMONIZER_TASK_NAME task.")
    }
}