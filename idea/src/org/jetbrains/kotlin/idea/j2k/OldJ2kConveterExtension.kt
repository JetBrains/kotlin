package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.j2k.*

class OldJ2kConverterExtension : J2kConverterExtension() {
    override val isNewJ2k = false

    override fun createJavaToKotlinConverter(
        project: Project,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        OldJavaToKotlinConverter(project, settings, services)

    override fun createPostProcessor(formatCode: Boolean): PostProcessor =
        J2kPostProcessor(formatCode)
}