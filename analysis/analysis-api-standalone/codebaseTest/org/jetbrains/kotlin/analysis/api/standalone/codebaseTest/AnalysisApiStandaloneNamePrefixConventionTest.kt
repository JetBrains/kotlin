/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The test enforces the name convention for the Standalone Analysis API surface.
 */
class AnalysisApiStandaloneNamePrefixConventionTest : AbstractAnalysisApiCodebaseValidationTest() {
    @Test
    fun testNameConvention() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        for (declaration in psiFile.declarations) {
            if (declaration !is KtNamedDeclaration || !declaration.hasModifier(KtTokens.PUBLIC_KEYWORD) || declaration.hasDeprecatedAnnotation()) {
                continue
            }

            val declarationFqName = declaration.fqName?.asString() ?: continue
            if (declarationFqName in ignoredFqNames) {
                continue
            }

            val declarationName = declaration.name ?: continue

            if (!declarationName.isValidDeclarationName()) {
                error("No APIs in Standalone should have `standalone` in their name. '${declarationFqName}' from (${file.path}) violates this rule")

            }
            if (declaration !is KtClassLikeDeclaration) continue

            if (!declarationName.isValidClassName()) {
                error("All top-level classes have to have 'Ka' prefix. '${declarationFqName}' from (${file.path}) violates this rule")
            }
        }
    }

    /**
     * All classes in Analysis API must start with our `Ka` prefix
     */
    private fun String.isValidClassName() = startsWith("Ka")

    /**
     * It was decided to avoid using `standalone` for the endpoint names as
     * every declaration here is already contained in a `standalone` package.
     */
    private fun String.isValidDeclarationName() = !contains("standalone", ignoreCase = true)

    override val sourceDirectories: List<SourceDirectory.ForValidation> = listOf(
        SourceDirectory.ForValidation(
            sourcePaths = listOf(
                "src/org/jetbrains/kotlin/analysis/api/standalone",
            ),
        )
    )

    private companion object {
        /**
         * **DO NOT ADD NEW ENTRIES TO THIS LIST**
         *
         * The list of fully qualified names that violate the naming convention and have to be renamed.
         *
         * See KT-89023
         */
        private val ignoredFqNames = listOf(
            "org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession",
            "org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder",
            "org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi",
            "org.jetbrains.kotlin.analysis.api.standalone.projectStructure.StandaloneLibraryScopeConstructionMode",
            "org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession",
            "org.jetbrains.kotlin.analysis.api.standalone.disposeGlobalStandaloneApplicationServices"
        )
    }
}
