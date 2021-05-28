/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter

import org.jetbrains.kotlin.idea.fir.addExternalTestFiles
import org.jetbrains.kotlin.idea.highlighter.AbstractHighlightingTest
import org.jetbrains.kotlin.idea.fir.invalidateCaches
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.idea.fir.withPossiblyDisabledDuplicatedFirSourceElementsException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.utils.IgnoreTests

abstract class AbstractFirHighlightingTest : AbstractHighlightingTest() {
    override val captureExceptions: Boolean = false

    override fun getDefaultProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE

    override fun isFirPlugin() = true

    override fun doTest(unused: String?) {
        addExternalTestFiles(testPath())
        super.doTest(unused)
    }

    override fun tearDown() {
        project.invalidateCaches(file as? KtFile)
        super.tearDown()
    }

    override fun checkHighlighting(fileText: String) {
        val checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_INFOS_PREFIX);

        IgnoreTests.runTestIfNotDisabledByFileDirective(testDataFile().toPath(), IgnoreTests.DIRECTIVES.IGNORE_FIR) {
            // warnings are not supported yet
            withPossiblyDisabledDuplicatedFirSourceElementsException(fileText) {
                myFixture.checkHighlighting(/* checkWarnings= */ false, checkInfos, /* checkWeakWarnings= */ false)
            }
        }
    }
}