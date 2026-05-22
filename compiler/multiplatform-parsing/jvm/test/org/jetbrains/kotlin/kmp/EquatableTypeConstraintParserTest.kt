/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.toSourceLinesMapping
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquatableTypeConstraintParserTest {
    @Test
    fun parseEquatableTypeConstraints() {
        doParserTest("equatableTypeConstraints.kt", expectErrors = false)
    }

    @Test
    fun parseEquatableTypeConstraintRejectsTypeReference() {
        doParserTest("equatableTypeConstraintRejectsTypeReference.kt", expectErrors = true)
    }

    private fun doParserTest(fileName: String, expectErrors: Boolean) {
        val testDataPath = findTestDataPath(fileName)
        val text = testDataPath.readText()
        val root = NewTestParser(ParseMode.NoKDoc).parse(testDataPath.name, text)

        if (expectErrors) {
            assertTrue(root.countSyntaxElements().hasErrorElement)
        } else {
            assertFalse(root.countSyntaxElements().hasErrorElement)
        }
        val expectedPath = testDataPath.resolveSibling(testDataPath.nameWithoutExtension + ".txt")
        TestDataAssertions.assertEqualsToFile(expectedPath, root.dump(text.toSourceLinesMapping(), text))
    }

    private fun findTestDataPath(fileName: String): Path {
        val rootRelativePath = Paths.get("compiler/multiplatform-parsing/jvm/testData/parser/$fileName")
        val moduleRelativePath = Paths.get("jvm/testData/parser/$fileName")
        return listOf(rootRelativePath, moduleRelativePath)
            .first { it.exists() }
            .toAbsolutePath()
            .normalize()
    }
}
