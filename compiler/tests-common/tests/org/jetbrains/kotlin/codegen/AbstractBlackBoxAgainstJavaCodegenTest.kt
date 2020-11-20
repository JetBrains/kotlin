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

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

abstract class AbstractBlackBoxAgainstJavaCodegenTest : AbstractBlackBoxCodegenTest() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        javaClassesOutputDirectory = writeJavaFiles(files)!!.let { directory ->
            val jvmTargets = files.mapNotNullTo(mutableSetOf()) { it.directives["JVM_TARGET"]?.let((JvmTarget)::fromString) }
            check(jvmTargets.size <= 1) { "Having different JVM_TARGETs for different files is not supported in this test." }
            CodegenTestUtil.compileJava(
                CodegenTestUtil.findJavaSourcesInDirectory(directory), emptyList(), extractJavacOptions(files, jvmTargets.firstOrNull())
            )
        }

        super.doMultiFileTest(wholeFile, files.map { file ->
            // This is a hack which allows to avoid compiling Java sources for the second time (which would be incorrect in this test),
            // while also retaining content of all Java sources so that we could find and use test directives in Java sources
            // in CodegenTestCase.compile.
            if (file.name.endsWith(".java")) TestFile("${file.name}.disabled", file.content) else file
        })
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.addJvmClasspathRoot(javaClassesOutputDirectory)
    }
}
