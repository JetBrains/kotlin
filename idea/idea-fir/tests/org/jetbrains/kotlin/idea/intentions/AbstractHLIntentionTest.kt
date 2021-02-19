/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.test.uitls.IgnoreTests
import java.io.File

abstract class AbstractHLIntentionTest : AbstractIntentionTest() {
    override fun intentionFileName() = ".firIntention"

    override fun afterFileNameSuffix(ktFilePath: File): String {
        return if (ktFilePath.resolveSibling(ktFilePath.name + AFTER_FIR_EXTENSION).exists()) AFTER_FIR_EXTENSION
        else super.afterFileNameSuffix(ktFilePath)
    }

    override fun isFirPlugin() = true

    override fun doTestFor(mainFile: File, pathToFiles: Map<String, PsiFile>, intentionAction: IntentionAction, fileText: String) {
        IgnoreTests.runTestIfNotDisabledByFileDirective(mainFile.toPath(), IgnoreTests.DIRECTIVES.IGNORE_FIR) {
            super.doTestFor(mainFile, pathToFiles, intentionAction, fileText)
        }
    }

    override fun checkForErrorsAfter(fileText: String) {}
    override fun checkForErrorsBefore(fileText: String) {}

    companion object {
        private const val AFTER_FIR_EXTENSION = ".after.fir"
    }
}