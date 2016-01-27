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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class TestStdlibWithDxTest {
    @Test fun testRuntimeWithDx() {
        doTest(ForTestCompileRuntime.runtimeJarForTests())
    }

    @Test fun testReflectWithDx() {
        doTest(ForTestCompileRuntime.reflectJarForTests())
    }

    private fun doTest(file: File) {
        val zip = ZipInputStream(FileInputStream(file))
        zip.use {
            generateSequence { zip.nextEntry }.forEach {
                if (it.name.endsWith(".class")) {
                    DxChecker.checkFileWithDx(zip.readBytes(), it.name)
                }
            }
        }
    }
}
