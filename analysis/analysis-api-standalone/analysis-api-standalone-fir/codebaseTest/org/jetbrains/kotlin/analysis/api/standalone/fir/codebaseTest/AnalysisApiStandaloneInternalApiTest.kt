/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.codebaseTest

import org.jetbrains.kotlin.AbstractAnalysisApiInternalApiTest
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.KA_IMPLEMENTATION_DETAIL_ANNOTATION
import org.jetbrains.kotlin.psi.KtDeclaration
import org.junit.jupiter.api.Test

/**
 * Verifies that every public declaration in `analysis/analysis-api-standalone/analysis-api-standalone-fir/src` is marked. Suggests
 * `@KaImplementationDetail` for unmarked declarations.
 *
 * Declarations inside `org.jetbrains.kotlin.idea.references` package are ignored.
 *
 * See [AbstractAnalysisApiInternalApiTest] for the precise rules and rationale.
 */
class AnalysisApiStandaloneInternalApiTest : AbstractAnalysisApiInternalApiTest() {
    @Test
    fun testInternalApiMarking() = doTest()

    override val sourceDirectories: List<SourceDirectory.ForValidation> =
        listOf(SourceDirectory.ForValidation(sourcePaths = listOf("src")))

    override fun isExempt(declaration: KtDeclaration): Boolean {
        return declaration.containingKtFile.packageFqName.asString() == "org.jetbrains.kotlin.idea.references"
    }

    override fun suggestedAnnotation(declaration: KtDeclaration): String = KA_IMPLEMENTATION_DETAIL_ANNOTATION
}
