/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import org.jetbrains.kotlin.config.JDK_VALUE_CLASSES
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.streams.asSequence

class JdkValueClassesTest {
    @Test
    fun testJdkValueClassesAreTheValueClassesOfValhallaJdk() {
        assumeTrue(KtTestUtil.isJdkValhallaAvailable(), "JDK_VALHALLA is not set")
        // In preview mode, the file system of the JDK image provides the preview class files.
        val environment = mapOf("java.home" to KtTestUtil.getJdkValhallaHome().path, "previewMode" to "true")
        val valueClasses = FileSystems.newFileSystem(URI.create("jrt:/"), environment).use { fileSystem ->
            Files.walk(fileSystem.getPath("/modules")).use { paths ->
                paths.asSequence()
                    .filter { it.toString().endsWith(".class") }
                    .map { ClassReader(Files.readAllBytes(it)) }
                    .filter { it.isValueClass() }
                    .mapTo(sortedSetOf()) { it.className }
            }
        }
        assertEquals(JDK_VALUE_CLASSES.toSortedSet(), valueClasses)
    }

    // A value class is a class of a preview class file that is neither an interface nor a module, and lacks `ACC_IDENTITY`.
    private fun ClassReader.isValueClass(): Boolean =
        readUnsignedShort(4) == 0xFFFF && access and (Opcodes.ACC_INTERFACE or Opcodes.ACC_MODULE or Opcodes.ACC_SUPER) == 0
}
