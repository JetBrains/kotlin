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

import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractBlackBoxAgainstJavaCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun createEnvironment(jdkKind: TestJdkKind, javaSourceDir: File?) {
        val javaOutputDir = javaSourceDir?.let { javaSourceDir ->
            val javaSourceFilePaths = javaSourceDir.walk().mapNotNull { file ->
                if (file.isFile && file.extension == JavaFileType.DEFAULT_EXTENSION) {
                    file.path
                }
                else null
            }.toList()

            CodegenTestUtil.compileJava(javaSourceFilePaths, emptyList(), emptyList())
        }

        val configuration = KotlinTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, jdkKind, KotlinTestUtils.getAnnotationsJar(), javaOutputDir
        )

        myEnvironment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}
