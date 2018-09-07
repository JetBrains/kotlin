/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.project.Project
import java.util.*

internal object GradlePropertiesFileUtils {
    const val KOTLIN_CODE_STYLE_GRADLE_SETTING = "kotlin.code.style"

    private const val GRADLE_PROPERTIES_FILE_NAME = "gradle.properties"
    private const val GRADLE_PROPERTIES_LOCAL_FILE_NAME = "local.properties"

    private val gradlePropertyFiles = listOf(GRADLE_PROPERTIES_LOCAL_FILE_NAME, GRADLE_PROPERTIES_FILE_NAME)

    fun readProperty(project: Project, propertyName: String): String? {
        for (propertyFileName in gradlePropertyFiles) {
            val propertyFile = project.baseDir.findChild(propertyFileName) ?: continue
            val properties = Properties()
            properties.load(propertyFile.inputStream)
            properties.getProperty(propertyName)?.let {
                return it
            }
        }

        return null
    }
}