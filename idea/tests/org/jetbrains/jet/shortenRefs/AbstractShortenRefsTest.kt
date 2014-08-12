/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.shortenRefs

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.openapi.application.ApplicationManager
import java.io.File
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.codeInsight.CodeInsightSettings
import org.jetbrains.jet.InTextDirectivesUtils

public abstract class AbstractShortenRefsTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestCaseBuilder.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(testPath: String) {
        val codeInsightSettings = CodeInsightSettings.getInstance()
        val optimizeImportsBefore = codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY

        try {
            val fixture = myFixture
            val dependencyPath = testPath.replace(".kt", ".dependency.kt")
            if (File(dependencyPath).exists()) {
                fixture.configureByFile(dependencyPath)
            }
            val javaDependencyPath = testPath.replace(".kt", ".dependency.java")
            if (File(javaDependencyPath).exists()) {
                fixture.configureByFile(javaDependencyPath)
            }

            fixture.configureByFile(testPath)

            val file = fixture.getFile() as JetFile
            val selectionModel = fixture.getEditor().getSelectionModel()
            if (!selectionModel.hasSelection()) error("No selection in input file")

            codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY =
                    InTextDirectivesUtils.isDirectiveDefined(file.getText(), "// OPTIMIZE_IMPORTS")

            CommandProcessor.getInstance().executeCommand(getProject(), {
                ApplicationManager.getApplication()!!.runWriteAction {
                    ShortenReferences.process(file, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd())
                }
            }, null, null)
            selectionModel.removeSelection()

            fixture.checkResultByFile(testPath + ".after")
        }
        finally {
            codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY = optimizeImportsBefore
        }
    }
}