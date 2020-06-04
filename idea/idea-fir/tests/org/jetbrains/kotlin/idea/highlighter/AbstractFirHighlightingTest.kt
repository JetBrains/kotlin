/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFirHighlightingTest : AbstractHighlightingTest() {
    override val captureExceptions: Boolean = false

    override fun getDefaultProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE

    override fun isFirPlugin() = true

    override fun checkHighlighting(fileText: String) {
        val doComparison = !InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "IGNORE_FIR")
        val checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_INFOS_PREFIX);

        try {
            // warnings are not supported yet
            myFixture.checkHighlighting(/* checkWarnings= */ false, checkInfos, /* checkWeakWarnings= */ false)
        } catch (e: Throwable) {
            if (doComparison) throw e
            return
        }
        if (!doComparison) {
            throw AssertionError("Looks like test is passing, please remove IGNORE_FIR")
        }
    }
}