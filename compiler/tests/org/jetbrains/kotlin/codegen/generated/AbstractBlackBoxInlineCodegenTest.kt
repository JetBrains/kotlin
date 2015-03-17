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

package org.jetbrains.kotlin.codegen.generated

import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.jvm.compiler.AbstractSMAPBaseTest
import java.io.File

public abstract class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest(), AbstractSMAPBaseTest {

    public fun doTestMultiFileWithInlineCheck(firstFileName: String) {
        val fileName = relativePath(File(firstFileName))
        val inputFiles = listOf(fileName, fileName.substringBeforeLast("1.kt") + "2.kt")

        doTestMultiFile(inputFiles)
        try {
            InlineTestUtil.checkNoCallsToInline(initializedClassLoader.getAllGeneratedFiles())
            checkSMAP(myFiles.getPsiFiles(), generateClassesInFile().asList())
        }
        catch (e: Throwable) {
            System.out.println(generateToText())
            throw e
        }
    }
}
