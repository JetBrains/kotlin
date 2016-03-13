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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File

abstract class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun doMultiFileTest(file: File, files: List<TestFile>, javaFilesDir: File?) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL)
        loadMultiFiles(files)
        blackBox()

        try {
            InlineTestUtil.checkNoCallsToInline(initializedClassLoader.allGeneratedFiles.filterClassFiles(), myFiles.psiFiles)
            SMAPTestUtil.checkSMAP(files, generateClassesInFile().getClassFiles())
        }
        catch (e: Throwable) {
            println(generateToText())
            throw e
        }
    }
}
