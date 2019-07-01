package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.j2k.*

class OldJ2kConverterExtension : J2kConverterExtension() {
    override val isNewJ2k = false

    override fun createJavaToKotlinConverter(
        project: Project,
        targetModule: Module?,
        settings: ConverterSettings,
        services: JavaToKotlinConverterServices
    ): JavaToKotlinConverter =
        OldJavaToKotlinConverter(project, settings, services)

    override fun createPostProcessor(formatCode: Boolean): PostProcessor =
        J2kPostProcessor(formatCode)

    override fun createWithProgressProcessor(
        progress: ProgressIndicator?,
        files: List<PsiJavaFile>?,
        phasesCount: Int
    ): WithProgressProcessor =
        OldWithProgressProcessor(progress, files)
}