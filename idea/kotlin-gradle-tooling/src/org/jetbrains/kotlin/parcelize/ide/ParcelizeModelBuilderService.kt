/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ide

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.Serializable

interface ParcelizeGradleModel : Serializable {
    val isEnabled: Boolean
}

class ParcelizeGradleModelImpl(override val isEnabled: Boolean) : ParcelizeGradleModel

class ParcelizeModelBuilderService : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
            .withDescription("Unable to build kotlin-parcelize plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == ParcelizeGradleModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val parcelizePlugin: Plugin<*>? = project.plugins.findPlugin("kotlin-parcelize")

        return ParcelizeGradleModelImpl(isEnabled = parcelizePlugin != null)
    }
}