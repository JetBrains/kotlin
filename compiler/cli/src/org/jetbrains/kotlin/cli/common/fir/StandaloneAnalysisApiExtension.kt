/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.fir

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.psi.KtFile

interface StandaloneAnalysisApiExtension {
    companion object : ProjectExtensionDescriptor<StandaloneAnalysisApiExtension>(
        name = "org.jetbrains.kotlin.fir.analyzeApiExtension",
        StandaloneAnalysisApiExtension::class.java
    )

    fun runAnalysis(ktFiles: List<KtFile>, ktAnalysisSession: KtAnalysisSession)
}
