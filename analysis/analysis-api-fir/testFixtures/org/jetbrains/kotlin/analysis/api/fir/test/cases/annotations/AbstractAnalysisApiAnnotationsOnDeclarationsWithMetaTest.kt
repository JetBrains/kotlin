/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList

abstract class AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest : AbstractAnalysisApiAnnotationsOnDeclarationsTest() {
    context(session: KaSession)
    override fun renderAnnotations(annotations: KaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotationsWithMeta(annotations)
    }
}
