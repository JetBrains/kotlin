/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseDumpFileComparisonTest
import org.jetbrains.kotlin.analysis.api.codebaseTest.AnalysisApiSurfaceNames.IMPLEMENTATION_DETAIL
import org.jetbrains.kotlin.isPubliclyVisible
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test

class AnalysisApiLegacyCallableDumpTest : AbstractAnalysisApiCodebaseDumpFileComparisonTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf(
                "src/org/jetbrains/kotlin/analysis/api/components",
                "src/org/jetbrains/kotlin/analysis/api/KaSession.kt",
            ),
            "api/analysis-api.legacy-component-callables",
        ),
    )

    override fun PsiFile.processFile(): List<String> {
        if (this !is KtFile) return emptyList()


        return buildList {
            declarations.forEach {
                if (isToBeMigrated(it)) {
                    add(findSessionComponent()?.name + ": " + it.renderDeclaration())
                }
            }
        }
    }

    private fun isToBeMigrated(declaration: KtDeclaration): Boolean = when {
        declaration.hasDeprecatedAnnotation() -> false
        !declaration.isPubliclyVisible -> false
        declaration is KtClassOrObject -> {
            !declaration.hasAnnotation("KaObsoleteComponentApi") &&
                    !declaration.hasAnnotation(IMPLEMENTATION_DETAIL) &&
                    !declaration.isSessionComponent
        }

        else -> true
    }

    override fun SourceDirectory.ForDumpFileComparison.getErrorMessage(): String =
        """
            The set of public declarations under the legacy Analysis API surface (`components/` and `KaSession.kt`) changed
            (see `${getRoots()}`).

            New API must NOT be added here. Add new endpoints as top-level `context(session: KaSession)` declarations in a domain
            package — see the "Endpoint Architecture" section in `analysis/docs/contribution-guide/api-development.md`.

            If you intentionally moved or removed a declaration as part of the migration, update the expected dump in
            `${outputFilePath}`.
        """.trimIndent()

    @Test
    fun testComponentSurface() {
        doTest()
    }
}
