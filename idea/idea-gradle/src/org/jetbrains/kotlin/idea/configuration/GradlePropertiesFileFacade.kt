/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.gradle.model.ExternalProject
import java.io.File
import java.util.*

internal class GradlePropertiesFileFacade(private val baseDir: String) {

    fun readProperty(propertyName: String): String? {

        val baseVirtualDir = LocalFileSystem.getInstance().findFileByPath(baseDir) ?: return null

        for (propertyFileName in GRADLE_PROPERTY_FILES) {
            val propertyFile = baseVirtualDir.findChild(propertyFileName) ?: continue
            Properties().also { it.load(propertyFile.inputStream) }.getProperty(propertyName)?.let {
                return it
            }
        }

        return null
    }

    fun addCodeStyleProperty(value: String) {
        addProperty(KOTLIN_CODE_STYLE_GRADLE_SETTING, value)
    }

    fun addNotImportedCommonSourceSetsProperty() {
        addProperty(KOTLIN_NOT_IMPORTED_COMMON_SOURCE_SETS_SETTING, true.toString())
    }

    private fun addProperty(key: String, value: String) {
        val projectPropertiesFile = File(baseDir, GRADLE_PROPERTIES_FILE_NAME)

        val keyValue = "$key=$value"

        val updatedText = if (projectPropertiesFile.exists()) {
            projectPropertiesFile.readText() + System.lineSeparator() + keyValue
        } else {
            keyValue
        }

        projectPropertiesFile.writeText(updatedText)
    }

    companion object {

        fun forProject(project: Project) = GradlePropertiesFileFacade(ExternalSystemApiUtil.toCanonicalPath(project.basePath!!))

        fun forExternalProject(externalProject: ExternalProject) = GradlePropertiesFileFacade(externalProject.projectDir.canonicalPath)

        const val KOTLIN_CODE_STYLE_GRADLE_SETTING = "kotlin.code.style"
        const val KOTLIN_NOT_IMPORTED_COMMON_SOURCE_SETS_SETTING = "kotlin.import.noCommonSourceSets"

        private const val GRADLE_PROPERTIES_FILE_NAME = "gradle.properties"
        private const val GRADLE_PROPERTIES_LOCAL_FILE_NAME = "local.properties"

        private val GRADLE_PROPERTY_FILES = listOf(GRADLE_PROPERTIES_LOCAL_FILE_NAME, GRADLE_PROPERTIES_FILE_NAME)
    }
}