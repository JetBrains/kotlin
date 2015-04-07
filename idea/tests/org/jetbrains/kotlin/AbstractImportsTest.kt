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

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public abstract class AbstractImportsTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doTest(testPath: String) {
        val codeInsightSettings = CodeInsightSettings.getInstance()
        val codeStyleSettings = JetCodeStyleSettings.getInstance(getProject())
        val optimizeImportsBefore = codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY
        val nameCountToUseStarBefore = codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT

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
            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT = InTextDirectivesUtils.getPrefixedInt(file.getText(), "// NAME_COUNT_TO_USE_STAR_IMPORT:") ?: nameCountToUseStarImportDefault

            getProject().executeWriteCommand("") {
                doTest(file)
            }

            fixture.checkResultByFile(testPath + ".after")
        }
        finally {
            codeInsightSettings.OPTIMIZE_IMPORTS_ON_THE_FLY = optimizeImportsBefore
            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT = nameCountToUseStarBefore
        }
    }

    protected abstract fun doTest(file: JetFile)

    protected open val nameCountToUseStarImportDefault: Int
        get() = 1
}