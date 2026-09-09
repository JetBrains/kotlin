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

public interface StandaloneAnalysisAPISession {
    public val coreApplicationEnvironment: CoreApplicationEnvironment

    public val application: Application

    public val project: Project

    public val modulesWithFiles: Map<KaSourceModule, List<PsiFile>>
}
