/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.references.TestReferenceResolveResultRenderer.renderResolvedPsi
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.unwrapMultiReferences
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsReferenceToTest : AbstractAnalysisApiBasedTest() {
    private fun findReferencesAtCaret(mainKtFile: KtFile, caretPosition: Int): List<KtReference> =
        mainKtFile.findReferenceAt(caretPosition)?.unwrapMultiReferences().orEmpty().filterIsInstance<KtReference>()

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val carets = testServices.expressionMarkerProvider.getAllCarets(mainFile)
        carets.flatMap { findReferencesAtCaret(mainFile, it.offset) }.forEach { reference ->
            val resolved = reference.resolve() ?: testServices.assertions.fail { "Could not resolve reference at caret" }
            testServices.assertions.assertTrue(reference.isReferenceTo(resolved))
            testServices.assertions.assertEqualsToTestDataFileSibling(resolved.renderResolvedPsi(testServices))
        }
    }
}