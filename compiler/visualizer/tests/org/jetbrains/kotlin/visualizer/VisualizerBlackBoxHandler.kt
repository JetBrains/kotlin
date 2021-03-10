/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer

import org.jetbrains.kotlin.compiler.visualizer.FirVisualizer
import org.jetbrains.kotlin.compiler.visualizer.PsiVisualizer
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.dependencyProvider

class VisualizerBlackBoxHandler(private val psiTestServices: TestServices, private val firTestServices: TestServices) {
    private val assertions = psiTestServices.assertions

    fun process() {
        val psiDependencyProvider = psiTestServices.dependencyProvider
        val psiModule = psiDependencyProvider.getTestModule("main")
        val psiArtifact = psiDependencyProvider.getArtifact(psiModule, FrontendKinds.ClassicFrontend)

        val firDependencyProvider = firTestServices.dependencyProvider
        val firModule = firDependencyProvider.getTestModule("main")
        val firArtifact = firDependencyProvider.getArtifact(firModule, FrontendKinds.FIR)

        val psiRenderer = psiArtifact.ktFiles.values.firstOrNull()?.let { PsiVisualizer(it, psiArtifact.analysisResult) } ?: return
        val psiRenderResult = psiRenderer.render()

        val firRenderer = firArtifact.firFiles.values.firstOrNull()?.let { FirVisualizer(it) } ?: return
        val firRenderResult = firRenderer.render()

        assertions.assertEquals(psiRenderResult, firRenderResult)
    }
}