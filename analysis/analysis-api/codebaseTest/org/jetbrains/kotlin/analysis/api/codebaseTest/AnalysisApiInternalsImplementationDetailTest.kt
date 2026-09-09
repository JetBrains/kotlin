/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.KA_IMPLEMENTATION_DETAIL
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.KA_INTERNALS
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import java.io.File


/**
 * The test verifies the independent conventions that every top-level declaration in the `internals` package has to follow.
 *
 * @see assertImplementationDetailAnnotation
 * @see assertNamePrefix
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
            assertImplementationDetailAnnotation(declaration, file)
            assertNamePrefix(declaration, file)
        }
    }

    /**
     * Declarations in the `internals` package are implementation details with no compatibility guarantees and must not be used outside the
     * Analysis API implementation modules, so they all have to be annotated with `@KaImplementationDetail` accordingly.
     */
    private fun assertImplementationDetailAnnotation(declaration: KtDeclaration, file: File) {
        if (declaration.hasAnnotation(KA_IMPLEMENTATION_DETAIL)) return

        error(
            "All top-level declarations in the 'internals' package have to be annotated with '@$KA_IMPLEMENTATION_DETAIL'. " +
                    "'${declaration.name}' from (${file.path}) violates this rule"
        )
    }

    /**
     * Class-like declarations in the `internals` package start with the [KA_INTERNALS] prefix, named after the facade they are reached
     * through.
     */
    private fun assertNamePrefix(declaration: KtDeclaration, file: File) {
        // Callables cannot carry a type name prefix, so the convention can be formalized only for class-like declarations
        if (declaration !is KtClassLikeDeclaration) return

        val name = declaration.name
        if (name == null || name.startsWith(KA_INTERNALS)) return

        error(
            "All top-level classes in the 'internals' package have to have the '$KA_INTERNALS' prefix. " +
                    "'$name' from (${file.path}) violates this rule"
        )
    }
}
