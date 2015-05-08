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

package org.jetbrains.kotlin.idea.completion.test.handlers

import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.completion.test.COMPLETION_TEST_DATA_BASE_PATH
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import java.io.File
import kotlin.test.assertTrue

public class SmartCompletionMultifileHandlerTest : KotlinCompletionTestCase() {
    public fun testImportExtensionFunction() { doTest() }

    public fun testImportExtensionProperty() { doTest() }

    public fun testAnonymousObjectGenericJava() { doTest() }

    override fun setUp() {
        setType(CompletionType.SMART)
        super.setUp()
    }

    public fun doTest() {
        val fileName = getTestName(false)

        val fileNames = listOf(fileName + "-1.kt", fileName + "-2.kt", fileName + ".java")

        configureByFiles(null, *fileNames.filter { File(getTestDataPath() + it).exists() }.toTypedArray())

        complete(1)
        if (myItems != null) {
            assertTrue(myItems.size() == 1, "Multiple items in completion")
            selectItem(myItems[0])
        }

        checkResultByFile(fileName + ".kt.after")
    }

    override fun getTestDataPath() = File(COMPLETION_TEST_DATA_BASE_PATH, "/handlers/multifile/smart/").getPath() + File.separator
}
