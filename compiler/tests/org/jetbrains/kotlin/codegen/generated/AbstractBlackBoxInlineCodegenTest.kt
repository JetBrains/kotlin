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

import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.codegen.InlineTestUtil
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.jvm.compiler.AbstractSMAPBaseTest
import org.jetbrains.kotlin.test.ConfigurationKind
import java.io.File

abstract class AbstractBlackBoxInlineCodegenTest : AbstractBlackBoxCodegenTest(), AbstractSMAPBaseTest {
    override fun doMultiFileTest(
            file: File, modules: Map<String, KotlinMultiFileTestWithJava<Void, TestFile>.ModuleAndDependencies>, files: List<TestFile>
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL)
        loadMultiFiles(files)
        blackBox()

        try {
            InlineTestUtil.checkNoCallsToInline(initializedClassLoader.allGeneratedFiles.filterClassFiles(), myFiles.psiFiles)
            checkSMAP(files, generateClassesInFile().getClassFiles())
        }
        catch (e: Throwable) {
            System.out.println(generateToText())
            throw e
        }
    }
}
