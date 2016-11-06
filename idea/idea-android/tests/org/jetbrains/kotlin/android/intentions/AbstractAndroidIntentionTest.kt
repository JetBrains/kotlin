/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.intentions

import com.android.SdkConstants
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.android.inspections.klint.AndroidLintInspectionBase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


abstract class AbstractAndroidIntentionTest : KotlinAndroidTestCase() {

    fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)
        val intentionText = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ") ?: error("Empty intention text")
        val mainInspectionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INSPECTION_CLASS: ") ?: error("Empty inspection class name")

        val inspectionClass = Class.forName(mainInspectionClassName).newInstance() as AndroidLintInspectionBase

        myFixture.enableInspections(inspectionClass)

        val sourceFile = myFixture.copyFileToProject(path, "src/${PathUtil.getFileName(path)}")
        myFixture.configureFromExistingVirtualFile(sourceFile)

        DirectiveBasedActionUtils.checkForUnexpectedErrors(myFixture.file as KtFile)

        val intention = myFixture.getAvailableIntention(intentionText) ?: error("Failed to find intention")
        myFixture.launchAction(intention)

        myFixture.checkResultByFile(path + ".expected")
    }

    override fun createManifest() {
        myFixture.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()
}