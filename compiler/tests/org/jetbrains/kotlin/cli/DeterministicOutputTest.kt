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
import kotlin.test.assertNotEquals
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil.DOS_EPOCH
import org.jetbrains.kotlin.test.TestCaseWithTmpdir

class DeterministicOutputTest : TestCaseWithTmpdir() {

    fun testDeterministicOutput() {
        val fooKt = tmpdir.resolve("foo.kt").also {
            it.writeText("class Foo")
        }

        val firstJar = tmpdir.resolve("first.jar")
        AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(fooKt.path, "-d", firstJar.path, "-include-runtime"))

        val secondJar = tmpdir.resolve("second.jar")
        AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(fooKt.path, "-d", secondJar.path, "-include-runtime"))

        assertEquals(
            firstJar.readBytes().toList(),
            secondJar.readBytes().toList(),
            "jar contents should be identical if compiler command and inputs are the same")
        assertAllTimestampsAreReset(firstJar)
        assertAllTimestampsAreReset(secondJar)
    }

    fun testNoResetJarTimestamps() {
        val fooKt = tmpdir.resolve("foo.kt").also {
            it.writeText("class Foo")
        }
        val jar = tmpdir.resolve("jarWithTimestamps.jar")
        AbstractCliTest.executeCompilerGrabOutput(
            K2JVMCompiler(),
            listOf(fooKt.path, "-d", jar.path, "-include-runtime", "-Xno-reset-jar-timestamps"))

        assertNoTimestampsAreReset(jar)
    }

    private fun assertAllTimestampsAreReset(jar: File) {
        val zis = JarInputStream(FileInputStream(jar))
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            assertEquals(entry.time, DOS_EPOCH, "$entry timestamp should be reset")
            entry = zis.nextEntry
        }
    }

    private fun assertNoTimestampsAreReset(jar: File) {
        val zis = JarInputStream(FileInputStream(jar))
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            assertNotEquals(entry.time, DOS_EPOCH, "$entry timestamp should not be reset")
            entry = zis.nextEntry
        }
    }
}
