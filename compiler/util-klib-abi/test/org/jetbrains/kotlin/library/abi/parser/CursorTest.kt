/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.library.abi.parser

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CursorTest {

    @Test
    fun cursorShowsCurrentLine() {
        val input = "one\ntwo\nthree"
        val cursor = Cursor(input)
        assertEquals("one", cursor.currentLine)
        cursor.nextLine()
        assertEquals("two", cursor.currentLine)
        cursor.nextLine()
        assertEquals("three", cursor.currentLine)
        cursor.nextLine()
        assertTrue(cursor.isFinished())
        assertTrue(cursor.isFinished())
    }

    @Test
    fun parseIdentifiersThatArePossibleByEscaping() {
        val validEscapedIdentifiers = listOf("123", "🙂", "identifiers can include spaces")
        for (id in validEscapedIdentifiers) {
            assertEquals(id, Cursor(id).parseValidIdentifier())
        }
    }

    @Test
    fun parseValidIdentifierDoesNotIncludeIllegalCharacters() {
        val identifiersIncludingIllegalChar =
            listOf(
                "identifiers can't include :",
                "identifiers can't include \\",
                "identifiers can't include /",
                "identifiers can't include ;",
                "identifiers can't include (",
                "identifiers can't include )",
                "identifiers can't include <",
                "identifiers can't include >",
                "identifiers can't include [",
                "identifiers can't include ]",
                "identifiers can't include {",
                "identifiers can't include }",
                "identifiers can't include ?",
                "identifiers can't include ,",
            )
        for (id in identifiersIncludingIllegalChar) {
            val cursor = Cursor(id)
            assertEquals("identifiers can't include ", cursor.parseValidIdentifier())
            assertEquals(id.last().toString(), cursor.currentLine)
        }
    }

    @Test
    fun parseIdentifierThatEndsWithASpaceAtEndOfLine() {
        val cursor = Cursor("identifiers can include spaces at the end  //")
        val id = cursor.parseValidIdentifier()
        assertEquals("identifiers can include spaces at the end  ", id)
        assertEquals("//", cursor.currentLine)
    }

    @Test
    fun parseIdentifierThatEndsWithASpaceAtEndOfFunctionName() {
        val cursor = Cursor("identifiers can include spaces at the end ()")
        val id = cursor.parseValidIdentifier()
        assertEquals("identifiers can include spaces at the end ", id)
        assertEquals("()", cursor.currentLine)
    }

    @Test
    fun skipWhitespace() {
        val input = "    test"
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertEquals("test", cursor.currentLine)
    }

    @Test
    fun skipWhitespaceOnBlankLine() {
        val input = ""
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertEquals("", cursor.currentLine)
    }

    @Test
    fun skipWhitespaceSkipsEntireLine() {
        val input = "    "
        val cursor = Cursor(input)
        cursor.skipInlineWhitespace()
        assertEquals("", cursor.currentLine)
    }
}
