/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.codebaseTest

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractAnalysisApiCodebaseValidationTest
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards the structural convention of the Analysis API surface (see KT-87578).
 *
 * In each surface class, members whose value is a genuine constant for that kind of declaration are declared as
 * `final override` and grouped at the very end of the class body inside a `//region Implementation details` …
 * `//endregion` fold, right after `createPointer()`. Members that vary between implementations must stay abstract and be
 * implemented per declaration, so they must not be pinned here.
 *
 * The test enforces, for every class under the surface:
 * - every `final override` member is inside the region, and the region contains nothing but `final override` members;
 * - no declaration appears after `//endregion` (the region is the last block);
 * - `createPointer()`, when declared, is the last declaration before the region opens.
 */
class AnalysisApiImplementationRegionTest : AbstractAnalysisApiCodebaseValidationTest() {
    override val sourceDirectories: List<SourceDirectory.ForValidation> = listOf(
        SourceDirectory.ForValidation(listOf("src")),
    )

    override fun processFile(file: File, psiFile: PsiFile) {
        if (psiFile !is KtFile) return

        val violations = buildList {
            for (klass in psiFile.collectDescendantsOfType<KtClassOrObject>()) {
                validateClass(klass, psiFile, this)
            }
        }

        if (violations.isNotEmpty()) {
            throw AssertionError(
                buildString {
                    appendLine("The symbol implementation-details region convention is violated:")
                    appendLine()
                    violations.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine(EXPLANATION)
                }
            )
        }
    }

    private fun validateClass(klass: KtClassOrObject, psiFile: KtFile, violations: MutableList<String>) {
        val body = klass.body ?: return
        val bodyStart = body.textRange.startOffset
        val bodyText = body.text

        val regionOffset = bodyText.indexOf(REGION_OPEN).takeIf { it >= 0 }?.let { bodyStart + it }
        val endRegionOffset = bodyText.indexOf(REGION_CLOSE).takeIf { it >= 0 }?.let { bodyStart + it }

        if (regionOffset != null && endRegionOffset == null) {
            violations += format(psiFile, regionOffset, "'$REGION_OPEN' in `${klass.name}` is not closed with '$REGION_CLOSE'")
            return
        }

        val declarationsBeforeRegion = mutableListOf<KtDeclaration>()

        for (declaration in klass.declarations) {
            // `textOffset` points at the name identifier, past any leading comment that the parser may have attached to the
            // declaration (e.g. the `//region` marker binding to the first member), so it reliably lands inside the region.
            val offset = declaration.textOffset
            val name = "${klass.name}.${declaration.name ?: declaration.text.take(40)}"

            val isFinalOverride = declaration is KtCallableDeclaration &&
                    declaration.hasModifier(KtTokens.FINAL_KEYWORD) &&
                    declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)

            val inRegion = regionOffset != null && offset > regionOffset && offset < endRegionOffset!!
            val afterRegion = endRegionOffset != null && offset > endRegionOffset

            when {
                afterRegion ->
                    violations += format(psiFile, offset, "`$name` is declared after '$REGION_CLOSE'; the region must be the last block")
                inRegion && !isFinalOverride ->
                    violations += format(psiFile, offset, "`$name` is inside '$REGION_OPEN' but is not a `final override`")
                !inRegion && isFinalOverride ->
                    violations += format(psiFile, offset, "`$name` is a `final override` but is outside '$REGION_OPEN'")
            }

            if (regionOffset != null && offset < regionOffset) {
                declarationsBeforeRegion += declaration
            }
        }

        if (regionOffset != null) {
            val createPointer = klass.declarations.firstOrNull { it is KtNamedFunction && it.name == CREATE_POINTER }
            if (createPointer != null && declarationsBeforeRegion.lastOrNull() != createPointer) {
                violations += format(
                    psiFile,
                    createPointer.textOffset,
                    "`${klass.name}.$CREATE_POINTER()` must be the last declaration before '$REGION_OPEN'",
                )
            }
        }
    }

    private fun format(psiFile: KtFile, offset: Int, message: String): String {
        val line = psiFile.text.substring(0, offset).count { it == '\n' } + 1
        return "${psiFile.name}:$line: $message"
    }

    @Test
    fun testImplementationDetailsRegion() {
        doTest()
    }

    private companion object {
        const val REGION_OPEN: String = "//region Implementation details"
        const val REGION_CLOSE: String = "//endregion"
        const val CREATE_POINTER: String = "createPointer"

        const val EXPLANATION: String =
            "Members whose value is a constant for that kind of declaration must be declared as `final override` and grouped " +
                    "at the end of the class inside a `$REGION_OPEN` … `$REGION_CLOSE` fold, right after `$CREATE_POINTER()`. " +
                    "Members that vary between implementations must stay abstract and be implemented per declaration instead of " +
                    "being pinned here."
    }
}
