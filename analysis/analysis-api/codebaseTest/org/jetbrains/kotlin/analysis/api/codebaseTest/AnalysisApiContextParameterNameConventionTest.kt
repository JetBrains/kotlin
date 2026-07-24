/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.forEachNonLocalPublicDeclaration
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.junit.jupiter.api.Test
import java.io.File


/**
 * The test enforces the name convention for the Analysis API Surface.
 *
 * In particular:
 * - All [org.jetbrains.kotlin.analysis.api.KaSession] context parameters must have the same `session` name.
 * - All context parameters must have some name (not `_`).
 */
class AnalysisApiContextParameterNameConventionTest : AbstractAnalysisApiSurfaceCodebaseValidationTest() {
    @Test
    fun testNameConvention() = doTest()

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        psiFile.forEachNonLocalPublicDeclaration(::validate)
    }

    private fun validate(declaration: KtDeclaration) {
        if (declaration !is KtCallableDeclaration) return

        declaration.contextParameters.forEach(::validateContextParameter)
    }

    private fun validateContextParameter(parameter: KtParameter) {
        val parameterName = parameter.name ?: error("Parameter name is null in ${parameter.location}")
        if (parameterName == "_") {
            error("Context parameter names are part of the public API, so they must not be '_' in ${parameter.location}")
        }

        val typeText = parameter.typeReference?.text
        when (typeText) {
            AnalysisApiSurfaceNames.KA_SESSION -> {
                if (parameterName != "session") {
                    error("Parameter name is not 'session' in ${parameter.location}")
                }
            }
        }
    }

    private val KtParameter.location: String
        get() = "${containingKtFile.virtualFilePath}\nfor '$text'\nin ${ownerDeclaration?.text}"
}
