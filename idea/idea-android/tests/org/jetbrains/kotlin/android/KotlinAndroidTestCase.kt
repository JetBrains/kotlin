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

package org.jetbrains.kotlin.android

import com.android.SdkConstants
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File


abstract class KotlinAndroidTestCase : AndroidTestCase() {
    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()

    override fun createManifest() {
        myFixture.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }

    fun copyResourceDirectoryForTest(path: String) {
        val testFile = File(path)
        if (testFile.isFile) {
            myFixture.copyDirectoryToProject(testFile.parent + "/res", "res")
        } else if (testFile.isDirectory) {
            myFixture.copyDirectoryToProject(testFile.path + "/res", "res")
        }
    }
}