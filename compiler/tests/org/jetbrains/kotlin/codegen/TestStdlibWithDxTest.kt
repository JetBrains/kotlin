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
    @Test
    fun testRuntimeWithDx() {
        doTest(ForTestCompileRuntime.runtimeJarForTests())
    }

    @Test
    fun testReflectWithDx() {
        doTest(ForTestCompileRuntime.reflectJarForTests())
    }

    private fun doTest(file: File) {
        val files = mutableListOf<Pair<ByteArray, String>>();
        ZipInputStream(FileInputStream(file)).use { zip ->
            for (entry in generateSequence { zip.nextEntry }) {
                if (entry.name.endsWith(".class") && !entry.name.startsWith("META-INF/")) {
                    val bytes = zip.readBytes()
                    DxChecker.checkFileWithDx(bytes, entry.name)
                    files.add(Pair(bytes, entry.name))
                }
            }
        }
        D8Checker.checkFilesWithD8(files)
    }
}
