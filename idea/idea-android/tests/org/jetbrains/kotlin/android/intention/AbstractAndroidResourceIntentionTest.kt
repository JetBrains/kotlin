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

package org.jetbrains.kotlin.android.intention

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.idea.jsonUtils.getString
import java.io.File


abstract class AbstractAndroidResourceIntentionTest : KotlinAndroidTestCase() {

    companion object {
        val COM_MYAPP_PACKAGE_PATH = "com/myapp/"
    }

    fun doTest(path: String) {
        val configFile = File(path)
        val testDataPath = configFile.parent

        myFixture.testDataPath = testDataPath

        val config = JsonParser().parse(FileUtil.loadFile(File(path), true)) as JsonObject

        val intentionClass = if (config.has("intentionClass")) config.getString("intentionClass") else null
        val intentionText = if (config.has("intentionText")) config.getString("intentionText") else null
        val isApplicableExpected = if (config.has("isApplicable")) config.get("isApplicable").asBoolean else true
        val rFile = if (config.has("rFile")) config.get("rFile").asString else null
        val resDirectory = if (config.has("resDirectory")) config.get("resDirectory").asString else null

        if (rFile != null) {
            myFixture.copyFileToProject(rFile, "gen/$COM_MYAPP_PACKAGE_PATH" + PathUtil.getFileName(rFile))
        }
        else {
            if (File(testDataPath + "/R.java").isFile) {
                myFixture.copyFileToProject("R.java", "gen/${COM_MYAPP_PACKAGE_PATH}R.java")
            }
        }

        if (resDirectory != null) {
            myFixture.copyDirectoryToProject(resDirectory, "res")
        }
        else {
            if (File(testDataPath + "/res").isDirectory) {
                myFixture.copyDirectoryToProject("res", "res")
            }
        }

        val sourceFile = myFixture.copyFileToProject("main.kt", "src/main.kt")
        myFixture.configureFromExistingVirtualFile(sourceFile)

        val intentionAction: IntentionAction?
        if (intentionClass != null) {
            intentionAction = Class.forName(intentionClass).newInstance() as IntentionAction
        }
        else if (intentionText != null) {
            intentionAction = myFixture.getAvailableIntention(intentionText)
            if (intentionAction != null && !isApplicableExpected) {
                TestCase.fail("Intention action should not be available")
            }
        }
        else {
            intentionAction = null
        }

        if (intentionAction == null) {
            if (isApplicableExpected) {
                TestCase.fail("Couldn't find intention action")
            }
            return
        }

        TestCase.assertEquals(isApplicableExpected, intentionAction.isAvailable(myFixture.project, myFixture.editor, myFixture.file))
        if (!isApplicableExpected) {
            return
        }

        val element = getTargetElement()
        element?.putUserData(CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY, "resource_id")

        myFixture.launchAction(intentionAction)

        FileDocumentManager.getInstance().saveAllDocuments()

        myFixture.checkResultByFile("/expected/main.kt")
        assertResourcesEqual(testDataPath + "/expected/res")
    }

    fun assertResourcesEqual(expectedPath: String) {
        PlatformTestUtil.assertDirectoriesEqual(LocalFileSystem.getInstance().findFileByPath(expectedPath), getResourceDirectory())
    }

    fun getResourceDirectory() = LocalFileSystem.getInstance().findFileByPath(myFixture.tempDirPath + "/res")

    fun getTargetElement() = myFixture.file.findElementAt(myFixture.caretOffset)?.parent
}