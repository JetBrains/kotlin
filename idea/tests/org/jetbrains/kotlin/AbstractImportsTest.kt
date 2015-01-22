/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin

import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.codeInsight.CodeInsightSettings
import org.jetbrains.kotlin.idea.formatter.JetCodeStyleSettings
import java.io.File
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.idea.codeInsight.ShortenReferences

public abstract class AbstractImportsTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(testPath: String) {
        val codeInsightSettings = CodeInsightSettings.getInstance()
        val codeStyleSettings = JetCodeStyleSettings.getInstance(getProject())
        val optimizeImportsBefore = codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY
        val preferAllUnderBefore = codeStyleSettings.PREFER_ALL_UNDER_IMPORTS

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

            codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY = InTextDirectivesUtils.getPrefixedBoolean(file.getText(), "// OPTIMIZE_IMPORTS:") ?: false
            codeStyleSettings.PREFER_ALL_UNDER_IMPORTS = InTextDirectivesUtils.getPrefixedBoolean(file.getText(), "// ALL_UNDER_IMPORTS:") ?: preferAllUnderImportsDefault

            CommandProcessor.getInstance().executeCommand(getProject(), {
                ApplicationManager.getApplication()!!.runWriteAction {
                    doTest(file)
                }
            }, null, null)

            fixture.checkResultByFile(testPath + ".after")
        }
        finally {
            codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY = optimizeImportsBefore
            codeStyleSettings.PREFER_ALL_UNDER_IMPORTS = preferAllUnderBefore
        }
    }

    protected abstract fun doTest(file: JetFile)

    protected open val preferAllUnderImportsDefault: Boolean
        get() = true
}