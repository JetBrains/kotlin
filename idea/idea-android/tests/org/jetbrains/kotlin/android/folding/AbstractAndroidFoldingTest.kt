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

package org.jetbrains.kotlin.android.folding

import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import java.io.File


abstract class AbstractAndroidResourceFoldingTest : KotlinAndroidTestCase() {

    fun doTest(path: String) {
        val testFile = File(path)
        myFixture.copyFileToProject("${testFile.parent}/values.xml", "res/values/values.xml")
        myFixture.copyFileToProject("${testFile.parent}/R.java", "gen/com/myapp/R.java")
        myFixture.testFoldingWithCollapseStatus(path, "${myFixture.tempDirPath}/src/main.kt")
    }
}