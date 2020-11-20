/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.TestWithWorkingDir
import org.junit.Test
import java.io.File

internal class IncrementalFileToPathConverterTest : TestWithWorkingDir() {
    val separator: String = File.separator

    @Test
    fun testPathTransform() {
        val relativeFilePath = "testFile.txt"
        val transformedPath = testPathTransformation(workingDir.resolve("testDir"), relativeFilePath)

        assertEquals("${'$'}PROJECT_DIR${'$'}$separator$relativeFilePath", transformedPath)
    }

    @Test
    fun testComplicatedProjectRootPath() {
        val relativeFilePath = "testFile.txt"
        val transformedPath = testPathTransformation(workingDir.resolve("first$separator..${separator}testDir"), relativeFilePath)

        assertEquals("${'$'}PROJECT_DIR${'$'}$separator$relativeFilePath", transformedPath)
    }

    @Test
    fun testInccorectProjectRootPath() {
        val relativeFilePath = "testFile.txt"
        val transformedPath = testPathTransformation(workingDir.resolve("testDir$separator"), relativeFilePath)

        assertEquals("${'$'}PROJECT_DIR${'$'}$separator$relativeFilePath", transformedPath)
    }

    @Test
    fun testFileOutOfProject() {
        val relativeFilePath = "..${separator}testFile.txt"
        val transformedPath = testPathTransformation(workingDir.resolve("testDir"), relativeFilePath)

        assertEquals("${workingDir.absolutePath}${separator}testFile.txt", transformedPath)
    }

    @Test
    fun testFileWithExtraSlash() {
        val relativeFilePath = "testFile.txt$separator"
        val transformedPath = testPathTransformation(workingDir.resolve("testDir"), relativeFilePath)

        assertEquals("${'$'}PROJECT_DIR${'$'}${separator}testFile.txt", transformedPath)
    }

    private fun testPathTransformation(projectRoot: File, relativeFilePath: String): String {
        val pathConverter = IncrementalFileToPathConverter(projectRoot)
        val testFile = projectRoot.resolve(relativeFilePath)
        val transformedPath = pathConverter.toPath(testFile)
        assertEquals(testFile.normalize().absolutePath, pathConverter.toFile(transformedPath).normalize().absolutePath)
        return transformedPath
    }
}