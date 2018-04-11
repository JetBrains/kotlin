/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.quickfix

import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.android.inspections.lint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File


abstract class AbstractAndroidLintQuickfixTest : KotlinAndroidTestCase() {

    override fun setUp() {
        AndroidLintInspectionBase.invalidateInspectionShortName2IssueMap()
        super.setUp()
    }

    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val intentionText = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ") ?: error("Empty intention text")
        val mainInspectionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INSPECTION_CLASS: ") ?: error("Empty inspection class name")
        val dependency = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// DEPENDENCY: ")
        val intentionAvailable = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// INTENTION_NOT_AVAILABLE")

        val inspection = Class.forName(mainInspectionClassName).newInstance() as InspectionProfileEntry
        myFixture.enableInspections(inspection)

        val sourceFile = myFixture.copyFileToProject(path, "src/${PathUtil.getFileName(path)}")
        myFixture.configureFromExistingVirtualFile(sourceFile)

        if (dependency != null) {
            val (dependencyFile, dependencyTargetPath) = dependency.split(" -> ").map(String::trim)
            myFixture.copyFileToProject("${PathUtil.getParentPath(path)}/$dependencyFile", "src/$dependencyTargetPath")
        }

        if (intentionAvailable) {
            val intention = myFixture.getAvailableIntention(intentionText) ?: error("Failed to find intention")
            myFixture.launchAction(intention)
            myFixture.checkResultByFile(path + ".expected")
        }
        else {
            assertNull("Intention should not be available", myFixture.availableIntentions.find { it.text == intentionText })
        }
    }
}