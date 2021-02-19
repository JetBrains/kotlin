/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.shortenRefs

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.uitls.IgnoreTests

abstract class AbstractFirShortenRefsTest : AbstractImportsTest() {
    override val captureExceptions: Boolean = false

    override fun doTest(file: KtFile): String? {
        val selectionModel = myFixture.editor.selectionModel
        if (!selectionModel.hasSelection()) error("No selection in input file")

        val selection = runReadAction { TextRange(selectionModel.selectionStart, selectionModel.selectionEnd) }

        val shortenings = executeOnPooledThread {
            runReadAction {
                analyse(file) {
                    collectPossibleReferenceShortenings(file, selection)
                }
            }
        }

        project.executeWriteCommand("") {
            shortenings.invokeShortening()
        }

        selectionModel.removeSelection()
        return null
    }

    override val runTestInWriteCommand: Boolean = false

    protected fun doTestWithMuting(unused: String) {
        IgnoreTests.runTestIfEnabledByFileDirective(testDataFile().toPath(), IgnoreTests.DIRECTIVES.FIR_COMPARISON, ".after") {
            doTest(unused)
        }
    }

    override val nameCountToUseStarImportDefault: Int
        get() = Integer.MAX_VALUE
}

private fun <R> executeOnPooledThread(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { action() }.get()
