/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.codebaseTest

import org.jetbrains.kotlin.AbstractAnalysisApiInternalApiTest
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.KA_IMPLEMENTATION_DETAIL_ANNOTATION
import org.jetbrains.kotlin.AnalysisApiNonPublicMarkers.LL_FIR_INTERNALS_ANNOTATION
import org.jetbrains.kotlin.psi.KtDeclaration
import org.junit.jupiter.api.Test

/**
 * Verifies that every public declaration in `analysis/low-level-api-fir/src` is marked. Declarations in package
 * `org.jetbrains.kotlin.analysis.low.level.api.fir.api` (and its subpackages) get `@KaImplementationDetail`. Everything else gets
 * `@LLFirInternals`.
 *
 * See [AbstractAnalysisApiInternalApiTest] for the precise rules and rationale.
 */
class LLInternalApiTest : AbstractAnalysisApiInternalApiTest() {
    @Test
    fun testInternalApiMarking() = doTest()

    override val sourceDirectories: List<SourceDirectory.ForValidation> =
        listOf(SourceDirectory.ForValidation(sourcePaths = listOf("src")))

    override fun suggestedAnnotation(declaration: KtDeclaration): String {
        val packageName = declaration.containingKtFile.packageFqName.asString()
        val isPublicApiPackage = packageName == LL_FIR_PUBLIC_PACKAGE || packageName.startsWith("$LL_FIR_PUBLIC_PACKAGE.")
        return if (isPublicApiPackage) KA_IMPLEMENTATION_DETAIL_ANNOTATION else LL_FIR_INTERNALS_ANNOTATION
    }

    private companion object {
        private const val LL_FIR_PUBLIC_PACKAGE = "org.jetbrains.kotlin.analysis.low.level.api.fir.api"
    }
}
