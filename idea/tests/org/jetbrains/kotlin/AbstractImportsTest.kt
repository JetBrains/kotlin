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
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.PackageEntry
import org.jdom.Element
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
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
        val settingManager = CodeStyleSettingsManager.getInstance()
        val tempSettings = settingManager.getCurrentSettings().clone()
        settingManager.setTemporarySettings(tempSettings)

        val codeStyleSettings = JetCodeStyleSettings.getInstance(getProject())

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

            val fileText = file.getText()

            codeStyleSettings.NAME_COUNT_TO_USE_STAR_IMPORT = InTextDirectivesUtils.getPrefixedInt(fileText, "// NAME_COUNT_TO_USE_STAR_IMPORT:") ?: nameCountToUseStarImportDefault
            codeStyleSettings.IMPORT_PACKAGES = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// IMPORT_PACKAGES:") ?: true
            codeStyleSettings.IMPORT_NESTED_CLASSES = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// IMPORT_NESTED_CLASSES:") ?: false

            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGE_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), false))
            }
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PACKAGES_TO_USE_STAR_IMPORTS:").forEach {
                codeStyleSettings.PACKAGES_TO_USE_STAR_IMPORTS.addEntry(PackageEntry(false, it.trim(), true))
            }

            getProject().executeWriteCommand("") {
                doTest(file)
            }

            JetTestUtils.assertEqualsToFile(File(testPath + ".after"), myFixture.getFile().getText())
        }
        finally {
            settingManager.dropTemporarySettings()
        }
    }

    protected abstract fun doTest(file: JetFile)

    protected open val nameCountToUseStarImportDefault: Int
        get() = 1
}