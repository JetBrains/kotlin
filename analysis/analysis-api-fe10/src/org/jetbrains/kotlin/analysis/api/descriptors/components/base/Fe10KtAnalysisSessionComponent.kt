/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components.base

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession

interface Fe10KtAnalysisSessionComponent {
    val analysisSession: KtFe10AnalysisSession

    val analysisContext: Fe10AnalysisContext
        get() = analysisSession.analysisContext
}