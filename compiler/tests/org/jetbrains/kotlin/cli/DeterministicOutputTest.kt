/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli

import java.io.File
import java.io.FileInputStream
import java.util.jar.JarInputStream
import java.util.zip.ZipEntry
import kotlin.test.assertEquals
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil.DOS_EPOCH
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase

class DeterministicOutputTest : KtUsefulTestCase() {

    fun testDeterministicOutput() {
        val directory = KotlinTestUtils.getTestDataPathBase() + "/deterministicOutput"
        val librarySource = File(directory, "A.kt").also {
            it.writeText("class A")
        }

        val firstJar = File(directory, "first.jar")
        AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(librarySource.path, "-d", firstJar.path, "-include-runtime"))

        val secondJar = File(directory, "second.jar")
        AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(librarySource.path, "-d", secondJar.path, "-include-runtime"))

        assertEquals(
            firstJar.readBytes().toList(),
            secondJar.readBytes().toList(),
            "jar contents should be identical if compiler command and inputs are the same")
        assertAllTimestampsAreReset(firstJar)
        assertAllTimestampsAreReset(secondJar)
    }

    private fun assertAllTimestampsAreReset(jar: File) {
        val zis = JarInputStream(FileInputStream(jar))
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            assertEquals(entry.time, DOS_EPOCH, "timestamp should be default $entry")
            entry = zis.nextEntry
        }
    }
}
