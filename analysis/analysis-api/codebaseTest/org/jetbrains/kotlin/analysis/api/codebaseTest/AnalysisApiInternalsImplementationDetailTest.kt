/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.KA_IMPLEMENTATION_DETAIL
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import java.io.File


/**
 * The test verifies that every top-level declaration in the `internals` package is annotated with `@KaImplementationDetail`.
 *
 * Declarations in this package are implementation details with no compatibility guarantees and must not be used outside the
 * Analysis API implementation modules, so they all have to be marked accordingly.
 */
class AnalysisApiInternalsImplementationDetailTest : AbstractAnalysisApiCodebaseValidationTest() {
    override val sourceDirectories = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf("src/org/jetbrains/kotlin/analysis/api/internals"),
        )
    )

    @Test
    fun testImplementationDetail() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        for (declaration in psiFile.declarations) {
            if (!declaration.hasAnnotation(KA_IMPLEMENTATION_DETAIL)) {
                error(
                    "All top-level declarations in the 'internals' package have to be annotated with '@$KA_IMPLEMENTATION_DETAIL'. " +
                            "'${declaration.name}' from (${file.path}) violates this rule"
                )
            }
        }
    }
}
