/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.test

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import java.io.File


/**
 * The test enforces the name convention for the Analysis API Surface.
 */
class AnalysisApiNameConventionTest : AbstractAnalysisApiSurfaceCodebaseValidationTest() {
    @Test
    fun testNameConvention() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        for (declaration in psiFile.declarations) {
            // Currently, the naming convention can be formalized only for class-like declarations
            if (declaration !is KtClassLikeDeclaration) continue

            // It is fine to not have the prefix as long as the declaration is not exposed
            if (!declaration.hasModifier(KtTokens.PUBLIC_KEYWORD)) continue

            if (declaration.name?.startsWith("Ka") == false && declaration.fqName?.asString() !in ignoredFqNames) {
                error("All top-level classes have to have 'Ka' prefix. '${declaration.name}' from (${file.path}) violates this rule")
            }
        }
    }

    private companion object {
        /**
         * **DO NOT ADD NEW ENTRIES TO THIS LIST**
         *
         * The list of fully qualified names that violate the naming convention and have to be renamed.
         */
        private val ignoredFqNames = listOf(
            "org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds", // KT-82438

            "org.jetbrains.kotlin.analysis.api.components.DebuggerExtension", // KT-82439

            // KT-82440
            "org.jetbrains.kotlin.analysis.api.components.ShortenOptions",
            "org.jetbrains.kotlin.analysis.api.components.ShortenStrategy",
            "org.jetbrains.kotlin.analysis.api.components.TypeToShortenInfo",
            "org.jetbrains.kotlin.analysis.api.components.QualifierToShortenInfo",
            "org.jetbrains.kotlin.analysis.api.components.ThisLabelToShortenInfo",
            "org.jetbrains.kotlin.analysis.api.components.ShortenCommand",

            "org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue", // KT-82441

            "org.jetbrains.kotlin.analysis.api.symbols.AdditionalKDocResolutionProvider", // KT-82442

            "org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer", // KT-82443
        )
    }
}
