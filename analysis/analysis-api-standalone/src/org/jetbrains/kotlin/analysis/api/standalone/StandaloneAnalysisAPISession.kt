/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

public class StandaloneAnalysisAPISession internal constructor(
    kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
    public val createPackagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    modulesWithFilesProvider: () -> Map<KtSourceModule, List<PsiFile>>
) {
    // TODO: better to limit exposure? Current usages are: addExtension, jarFileSystem
    public val coreApplicationEnvironment: CoreApplicationEnvironment = kotlinCoreProjectEnvironment.environment

    public val application: Application = kotlinCoreProjectEnvironment.environment.application

    public val project: Project = kotlinCoreProjectEnvironment.project

    @Deprecated(
        "Use session builder's service registration.",
        ReplaceWith("project")
    )
    public val mockProject: MockProject = kotlinCoreProjectEnvironment.project

    public val modulesWithFiles: Map<KtSourceModule, List<PsiFile>> by lazy(modulesWithFilesProvider)
}