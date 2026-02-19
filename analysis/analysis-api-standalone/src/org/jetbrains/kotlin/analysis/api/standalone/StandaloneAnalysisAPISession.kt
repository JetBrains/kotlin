/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment

public class StandaloneAnalysisAPISession internal constructor(
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    modulesWithFilesProvider: () -> Map<KaSourceModule, List<PsiFile>>
) {
    // TODO: better to limit exposure? Current usages are: addExtension, jarFileSystem
    public val coreApplicationEnvironment: CoreApplicationEnvironment = kotlinCoreProjectEnvironment.environment

    public val application: Application = kotlinCoreProjectEnvironment.environment.application

    public val project: Project = kotlinCoreProjectEnvironment.project

    public val modulesWithFiles: Map<KaSourceModule, List<PsiFile>> by lazy(modulesWithFilesProvider)
}