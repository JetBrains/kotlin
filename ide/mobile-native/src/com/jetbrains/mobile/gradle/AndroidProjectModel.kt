/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import java.io.Serializable
import kotlin.reflect.full.memberFunctions

interface AndroidProjectModel : Serializable {
    val testInstrumentationRunner: String
}

data class AndroidProjectModelImpl(override val testInstrumentationRunner: String) : AndroidProjectModel

class AndroidProjectModelBuilder : ModelBuilderService {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder =
        ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build Android project configuration")

    override fun canBuild(modelName: String?): Boolean =
        modelName == AndroidProjectModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): AndroidProjectModel? {
        val id = getTestRunnerId(project) ?: return null
        return AndroidProjectModelImpl(id)
    }

    private fun getTestRunnerId(project: Project): String? {
        val extension = try {
            project.extensions.getByName("android")
        } catch (e: Throwable) {
            return null
        }
        val getDefaultConfig = extension::class.memberFunctions.find { it.name == "getDefaultConfig" }
            ?: return null
        val defaultConfig = getDefaultConfig.call(extension) ?: return null

        val getTestInstrumentationRunner = defaultConfig::class.memberFunctions.find { it.name == "getTestInstrumentationRunner" }
            ?: return null
        return getTestInstrumentationRunner.call(defaultConfig) as? String
    }
}

@Order(ExternalSystemConstants.UNORDERED)
class AndroidProjectResolver : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses() = setOf(AndroidProjectModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(AndroidProjectModelBuilder::class.java, Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        try {
            val model = resolverCtx.getExtraProject(gradleModule, AndroidProjectModel::class.java)
            if (model != null) {
                ideModule.createChild(KEY, model)
            }
        } finally {
            super.populateModuleExtraModels(gradleModule, ideModule)
        }
    }

    companion object {
        val KEY = Key.create(AndroidProjectModel::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}