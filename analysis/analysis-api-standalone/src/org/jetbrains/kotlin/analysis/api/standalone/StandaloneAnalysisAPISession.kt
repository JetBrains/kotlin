/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule

/**
 * Standalone Analysis API environment.
 *
 * [StandaloneAnalysisAPISession] is the main entity of the standalone Analysis API.
 * Users are expected to set it up through [buildStandaloneAnalysisAPISession] and
 * then use [modulesWithFiles] or [allModules] as anchors for calling the
 * [analyze][org.jetbrains.kotlin.analysis.api.session.analyze] entrypoint.
 *
 * Each analyzed project is supposed to have a single [StandaloneAnalysisAPISession] instance
 * that represents the given project structure.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface StandaloneAnalysisAPISession {
    public val application: Application

    public val project: Project

    /**
     * Maps [KaSourceModule] subset of [allModules] to the represented [PsiFile]s.
     */
    public val modulesWithFiles: Map<KaSourceModule, List<PsiFile>>

    /**
     * All [KaModule]s registered by the user, excluding [KaNotUnderContentRootModule]s and the built-ins module.
     */
    public val allModules: List<KaModule>

    @KaImplementationDetail
    public val coreApplicationEnvironment: CoreApplicationEnvironment
}
