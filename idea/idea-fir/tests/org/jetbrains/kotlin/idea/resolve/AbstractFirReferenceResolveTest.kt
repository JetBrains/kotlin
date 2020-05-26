/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.fir.FirResolution
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFirReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun isFirPlugin(): Boolean = true

    override fun getProjectDescriptor(): KotlinLightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    override fun setUp() {
        super.setUp()
        FirResolution.enabled = true
    }

    override fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureWithExtraFile(path, ".Data")
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "IGNORE_FIR")) {
            try {
                performChecks()
            } catch (t: Throwable) {
                return
            }
            throw AssertionError("Looks like test is passing, please remove IGNORE_FIR")
        }
        performChecks()
    }

    override fun tearDown() {
        FirResolution.enabled = false
        super.tearDown()
    }
}