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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractKapt3BuilderModeBytecodeShapeTest : CodegenTestCase() {
    private companion object {
        var TEST_LIGHT_ANALYSIS: ClassBuilderFactory = object : ClassBuilderFactories.TestClassBuilderFactory() {
            override fun getClassBuilderMode() = ClassBuilderMode.KAPT3
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: MutableList<TestFile>, javaFilesDir: File?) {
        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        compile(files, javaFilesDir)
        KotlinTestUtils.assertEqualsToFile(txtFile, BytecodeListingTextCollectingVisitor.getText(classFileFactory))
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        AnalysisHandlerExtension.registerExtension(environment.project, PartialAnalysisHandlerExtension())
    }

    override fun getClassBuilderFactory(): ClassBuilderFactory {
        return TEST_LIGHT_ANALYSIS
    }

    override fun verifyWithDex(): Boolean {
        return false
    }
}