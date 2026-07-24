/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.codebaseTest

import org.jetbrains.kotlin.AbstractAnalysisApiInternalApiTest
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.KA_IMPLEMENTATION_DETAIL_ANNOTATION
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.jupiter.api.Test

/**
 * Verifies that every public declaration in `analysis/analysis-api-fir/src` is marked. Suggests `@KaImplementationDetail` for unmarked
 * declarations.
 *
 * See [AbstractAnalysisApiInternalApiTest] for the precise rules and rationale.
 */
class AnalysisApiFirInternalApiTest : AbstractAnalysisApiInternalApiTest() {
    @Test
    fun testInternalApiMarking() = doTest()

    override val sourceDirectories: List<SourceDirectory.ForValidation> =
        listOf(SourceDirectory.ForValidation(sourcePaths = listOf("src")))

    override fun suggestedAnnotation(declaration: KtDeclaration): String = KA_IMPLEMENTATION_DETAIL_ANNOTATION

    override fun isExempt(declaration: KtDeclaration): Boolean {
        // The `KaCompilerPluginDiagnostic*` interfaces (and the legacy `Kt*` aliases) are part of the public Analysis API surface even
        // though they live in `analysis-api-fir`.
        val fqName = (declaration as? KtNamedDeclaration)?.fqName?.asString() ?: return false
        return fqName in EXEMPT_PUBLIC_DECLARATION_NAMES
    }

    companion object {
        private val EXEMPT_PUBLIC_DECLARATION_NAMES: Set<String> = buildSet {
            for (i in 0..4) {
                add("org.jetbrains.kotlin.analysis.api.fir.diagnostics.KaCompilerPluginDiagnostic$i")
                add("org.jetbrains.kotlin.analysis.api.fir.diagnostics.KtCompilerPluginDiagnostic$i")
            }
        }
    }
}
