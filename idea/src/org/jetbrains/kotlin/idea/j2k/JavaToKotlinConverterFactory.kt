/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.*

object JavaToKotlinConverterFactory {
    fun createJavaToKotlinConverter(
        project: Project,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        J2kConverterExtension.extension.createJavaToKotlinConverter(project, settings, services)

    fun createPostProcessor(formatCode: Boolean): PostProcessor =
        J2kConverterExtension.extension.createPostProcessor(formatCode)
}