/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.ide

import org.gradle.api.Project
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext
import java.io.File
import java.io.Serializable

interface LombokModel : Serializable {
    val configurationFile: File?
}

class LombokModelImpl(override val configurationFile: File?) : LombokModel

class LombokModelBuilderService : AbstractModelBuilderService() {
    private val extensionName = "kotlinLombok"

    override fun canBuild(modelName: String?): Boolean = modelName == LombokModel::class.java.name

    override fun buildAll(modelName: String, project: Project, context: ModelBuilderContext): Any? {
        if (project.plugins.findPlugin("kotlin-lombok") == null) return null

        val extension: Any = project.extensions.findByName(extensionName) ?: return null

        val configurationFile = extension.getFieldValue("configurationFile") as? File
        return LombokModelImpl(configurationFile)
    }

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build lombok plugin configuration")
    }

    private fun Any.getFieldValue(fieldName: String, clazz: Class<*> = this.javaClass): Any? {
        val field = clazz.declaredFields.firstOrNull { it.name == fieldName }
            ?: return getFieldValue(fieldName, clazz.superclass ?: return null)

        val oldIsAccessible = field.isAccessible
        try {
            field.isAccessible = true
            return field.get(this)
        } finally {
            field.isAccessible = oldIsAccessible
        }
    }
}
