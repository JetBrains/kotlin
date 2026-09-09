/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment

internal class StandaloneAnalysisAPISessionImpl(
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    modulesWithFilesProvider: () -> Map<KaSourceModule, List<PsiFile>>
) : StandaloneAnalysisAPISession {
    override val coreApplicationEnvironment: CoreApplicationEnvironment = kotlinCoreProjectEnvironment.environment

    override val application: Application = kotlinCoreProjectEnvironment.environment.application

    override val project: Project = kotlinCoreProjectEnvironment.project

    override val modulesWithFiles: Map<KaSourceModule, List<PsiFile>> by lazy(modulesWithFilesProvider)
}
