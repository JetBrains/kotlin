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

package org.jetbrains.jet.android

import com.intellij.openapi.application.PathManager
import com.android.SdkConstants
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import kotlin.test.fail

public abstract class AbstractAndroidGotoTest : KotlinAndroidTestCase() {

    override fun setUp() {
        System.setProperty(KotlinAndroidTestCaseBase.SDK_PATH_PROPERTY, PathManager.getHomePath() + "/androidSDK/")
        System.setProperty(KotlinAndroidTestCaseBase.PLATFORM_DIR_PROPERTY, "android-17")
        super.setUp()
    }

    override fun getTestDataPath(): String {
        return PluginTestCaseBase.getTestDataPathBase() + "/android/goto/" + getTestName(true) + "/"
    }

    override fun createManifest() {
        myFixture!!.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }

    override fun requireRecentSdk() = true

    public fun doTest(path: String) {
        val f = myFixture!!
        f.copyDirectoryToProject(getResDir()!!, "res")
        f.configureByFile(path + getTestName(true) + ".kt");

        val asf = GotoDeclarationAction.findTargetElement(f.getProject(), f.getEditor(), f.getCaretOffset())
        f.getEditor().toString()
        fail("this test doesn't work yet")
    }
}
