/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseDumpFileComparisonTest
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.isPubliclyVisible
import org.jetbrains.kotlin.psi.*
import org.junit.jupiter.api.Test

/**
 * Freezes the legacy Analysis API surface — the session component
 * interfaces under `components/` and the [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] facade — by dumping every public
 * declaration into a single master file and comparing against it.
 *
 * The Analysis API is migrating to the context-parameter-based endpoint architecture: new public API is added as top-level
 * `context(session: KaSession)` declarations in domain packages, NOT as members of session component interfaces mixed into
 * `KaSession`. See the "Endpoint Architecture" section in `analysis/docs/contribution-guide/api-development.md`.
 *
 * This test guards that migration: any declaration added to (or removed from) the legacy surface changes the dump and trips the
 * test, pointing the author at the new architecture.
 *
 * Deprecated declarations are left out of the dump, so it lists only the legacy surface that has not been retired yet. Members of a
 * deprecated container are still checked one by one, so a new member added to a deprecated component changes the dump.
 */
class AnalysisApiLegacySurfaceDumpTest : AbstractAnalysisApiCodebaseDumpFileComparisonTest() {
    override val sourceDirectories: List<SourceDirectory.ForDumpFileComparison> = listOf(
        SourceDirectory.ForDumpFileComparison(
            listOf(
                "src/org/jetbrains/kotlin/analysis/api/components",
                "src/org/jetbrains/kotlin/analysis/api/KaSession.kt",
            ),
            "api/analysis-api.legacy-surface",
        ),
    )

    override fun PsiFile.processFile(): List<String> {
        if (this !is KtFile) return emptyList()

        return buildList {
            collectNonDeprecatedPublicDeclarations(this@processFile)
        }
    }

    @OptIn(KtExperimentalApi::class)
    private fun MutableList<String>.collectNonDeprecatedPublicDeclarations(container: KtDeclarationContainer) {
        for (declaration in container.declarations) {
            if (!declaration.isPubliclyVisible) continue

            if (!declaration.isDeprecated()) {
                add(declaration.renderDeclaration())
            }

            if (declaration is KtDeclarationContainer) {
                collectNonDeprecatedPublicDeclarations(declaration)
            }
        }

        if (container is KtClassOrObject) {
            container.companionBlocks.forEach { collectNonDeprecatedPublicDeclarations(it) }
        }
    }

    /**
     * Whether the declaration itself is deprecated. A deprecation targeted at an accessor, a field or a parameter leaves the
     * declaration usable (e.g., through a callable reference), so such a declaration stays in the dump.
     */
    private fun KtDeclaration.isDeprecated(): Boolean = annotationEntries.any { annotation ->
        if (annotation.shortName?.asString() != Deprecated::class.simpleName) return@any false

        when (annotation.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null, AnnotationUseSiteTarget.ALL, AnnotationUseSiteTarget.PROPERTY -> true
            else -> false
        }
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
    fun testLegacySurface() {
        doTest()
    }
}
