/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest
import org.jetbrains.kotlin.test.utils.IgnoreTests
import java.io.File

abstract class AbstractFe10BindingIntentionTest : AbstractIntentionTest() {
    override fun isFirPlugin() = true

    // left empty because error reporting in FIR and old FE is different
    override fun checkForErrorsBefore(fileText: String) {}
    override fun checkForErrorsAfter(fileText: String) {}

    override fun doTestFor(mainFile: File, pathToFiles: Map<String, PsiFile>, intentionAction: IntentionAction, fileText: String) {
        IgnoreTests.runTestIfNotDisabledByFileDirective(mainFile.toPath(), IgnoreTests.DIRECTIVES.IGNORE_FE10_BINDING_BY_FIR, "after") {
            super.doTestFor(mainFile, pathToFiles, intentionAction, fileText)
        }
    }
}