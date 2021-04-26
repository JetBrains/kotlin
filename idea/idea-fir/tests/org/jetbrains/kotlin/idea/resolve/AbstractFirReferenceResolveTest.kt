/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.invalidateCaches
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.utils.IgnoreTests

abstract class AbstractFirReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun isFirPlugin(): Boolean = true

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    override fun tearDown() {
        project.invalidateCaches(myFixture.file as? KtFile)
        super.tearDown()
    }

    override fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureWithExtraFile(path, ".Data")

        IgnoreTests.runTestIfNotDisabledByFileDirective(testDataFile().toPath(), IgnoreTests.DIRECTIVES.IGNORE_FIR) {
            performChecks()
        }
    }
}