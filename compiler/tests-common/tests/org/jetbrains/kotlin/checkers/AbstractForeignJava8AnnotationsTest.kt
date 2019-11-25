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

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractForeignJava8AnnotationsTest : AbstractForeignAnnotationsTest() {
    override val annotationsPath: String
        get() = JAVA8_ANNOTATION_SOURCES_PATH
}

abstract class AbstractForeignJava8AnnotationsNoAnnotationInClasspathTest : AbstractForeignAnnotationsNoAnnotationInClasspathTest() {
    override val annotationsPath: String
        get() = JAVA8_ANNOTATION_SOURCES_PATH

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        if (skipForCompiledVersion(files)) return
        super.analyzeAndCheck(testDataFile, files)
    }
}

abstract class AbstractForeignJava8AnnotationsNoAnnotationInClasspathWithPsiClassReadingTest :
    AbstractForeignAnnotationsNoAnnotationInClasspathWithPsiClassReadingTest() {
    override val annotationsPath: String
        get() = JAVA8_ANNOTATION_SOURCES_PATH

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        if (skipForCompiledVersion(files)) return
        super.analyzeAndCheck(testDataFile, files)
    }
}

private fun skipForCompiledVersion(files: List<BaseDiagnosticsTest.TestFile>) =
        files.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SKIP_COMPILED_JAVA") }


private const val JAVA8_ANNOTATION_SOURCES_PATH = "third-party/jdk8-annotations"
