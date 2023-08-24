/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.TestWithWorkingDir
import org.junit.Test
import kotlin.test.assertFailsWith

class RelocatableFileToPathConverterTest : TestWithWorkingDir() {

    private lateinit var pathConverter: RelocatableFileToPathConverter

    override fun setUp() {
        super.setUp()
        pathConverter = RelocatableFileToPathConverter(workingDir)
    }

    @Test
    fun testToPath() {
        assertEquals("com/example/Foo.kt", pathConverter.toPath(workingDir.resolve("com/example/Foo.kt")))
    }

    @Test
    fun testToPathFails() {
        assertFailsWith(IllegalStateException::class) {
            pathConverter.toPath(workingDir.resolve("../outsideWorkingDir/com/example/Foo.kt").normalize())
        }
    }

    @Test
    fun testToFile() {
        assertEquals(workingDir.resolve("com/example/Foo.kt"), pathConverter.toFile("com/example/Foo.kt"))
    }

}