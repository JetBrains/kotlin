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

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File


abstract class AbstractForeignAnnotationsTest : AbstractDiagnosticsTest() {
    lateinit var annotationsFile: File

    override fun createCompilerConfiguration(javaFilesDir: File?): CompilerConfiguration {
        val annotationsFile = MockLibraryUtil.compileLibraryToJar(
                "compiler/testData/foreignAnnotations/annotations",
                "foreign-annotations", /* addSources = */false, /* allowKotlinPackage =*/ false)

        return KotlinTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY,
                TestJdkKind.MOCK_JDK,
                listOf(KotlinTestUtils.getAnnotationsJar(), annotationsFile),
                listOf(javaFilesDir))
    }
}