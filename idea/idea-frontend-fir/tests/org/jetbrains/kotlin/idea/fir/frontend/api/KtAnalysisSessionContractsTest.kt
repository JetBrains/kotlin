/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.Assert
import org.jetbrains.kotlin.idea.frontend.api.WriteActionStartInsideAnalysisContextException
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.analyseWithCustomToken
import org.jetbrains.kotlin.idea.frontend.api.tokens.AlwaysAccessibleValidityTokenFactory
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile

class KtAnalysisSessionContractsTest : KotlinLightCodeInsightFixtureTestCase() {
    override val captureExceptions: Boolean get() = false

    fun testThatWriteActionCannotBeCalledInsideAnalyseCall() {
        val fakeFile = createFakeKtFile()

        var wasThrown = false
        ApplicationManager.getApplication().invokeAndWait {
            @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
            hackyAllowRunningOnEdt {
                analyse(fakeFile) {
                    try {
                        runWriteAction { }
                    } catch (_: WriteActionStartInsideAnalysisContextException) {
                        wasThrown = true
                    }
                }
            }
        }
        Assert.assertTrue("WriteActionStartInsideAnalysisContextException was not thrown", wasThrown)
    }

    fun testThatWriteActionCannotBeCalledInsideAnalyseCallWithCustomToken() {
        val fakeFile = createFakeKtFile()

        var wasThrown = false
        ApplicationManager.getApplication().invokeAndWait {
            @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
            hackyAllowRunningOnEdt {
                analyseWithCustomToken(fakeFile, AlwaysAccessibleValidityTokenFactory) {
                    try {
                        runWriteAction { }
                    } catch (_: WriteActionStartInsideAnalysisContextException) {
                        wasThrown = true
                    }
                }
            }
        }
        Assert.assertTrue("WriteActionStartInsideAnalysisContextException was not thrown", wasThrown)
    }


    private fun createFakeKtFile(): KtFile =
        myFixture.configureByText("file.kt", "fun a() {}") as KtFile

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinLightProjectDescriptor.INSTANCE
}