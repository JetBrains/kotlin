/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.util.regex.Pattern
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestDataVariantsTest {
    @Test
    fun testVariant() {
        assertVariant("foo.ll.kt", "foo.kt")
        assertVariant("foo.ll.kts", "foo.kts", pattern = KT_OR_KTS)
        assertNotVariant("foo.kt", "foo.ll.kt")
    }

    @Test
    fun testVariantChain() {
        assertVariant("foo.ll.wasmJs.kt", "foo.kt")
        assertVariant("foo.ll.wasmJs.kt", "foo.ll.kt")
        assertVariant("foo.reversed.wasmWasi.kt", "foo.kt")
    }

    @Test
    fun testCompoundExtension() {
        assertVariant("foo.ll.test.kts", "foo.test.kts", pattern = KTS)
        assertVariant("foo.ll.repl.kts", "foo.repl.kts", pattern = KTS)
        assertVariant("foo.ll.wasmJs.repl.kts", "foo.repl.kts", pattern = KTS)
    }

    @Test
    fun testExcludedBase() {
        val excluded = Pattern.compile("^foo\\.kt$")
        assertFalse(matchesTestDataPattern("foo.kt", KT, excluded))
        assertTrue(matchesTestDataPattern("foo.ll.kt", KT, excluded))
        assertVariant("foo.ll.kt", "foo.kt")
    }

    @Test
    fun testOrphan() {
        assertNotVariant("foo.ll.kt")
        assertNotVariant("foo.ll.kt", "bar.kt", "foo.ll.wasmJs.kt")
        assertNotVariant("foo.ll.wasmJs.kt", "foo.wasmJs.ll.kt")
    }

    @Test
    fun testNumericParts() {
        assertNotVariant("kt385.109.441.kt", "kt385.kt", "kt385.109.kt", "kt385.441.kt")
        assertNotVariant("hiddenSuperOverrideIn1.0.kt", "hiddenSuperOverrideIn1.kt")
        assertNotVariant("l1a.4.kt", "l1a.kt")
        assertNotVariant("l1.0_init.kt", "l1.kt")
        assertNotVariant("foo.1.ll.kt", "foo.kt", "foo.ll.kt")
        assertVariant("foo.1.ll.kt", "foo.1.kt")
        assertVariant("1.2.ll.kt", "1.2.kt")
        assertNotVariant("1.2.kt", "1.kt")
    }

    @Test
    fun testPartStartingWithNonAsciiLetter() {
        assertVariant("foo.𐐀.kt", "foo.kt")
        assertVariant("foo.über.kt", "foo.kt")
        assertNotVariant("foo._ll.kt", "foo.kt")
    }

    @Test
    fun testFirstPartIsKept() {
        assertNotVariant("ll.kt", "kt")
        assertNotVariant("ll.foo.kt", "foo.kt")
        assertNotVariant(".ll.kt", ".kt", pattern = Pattern.compile("^(.*)\\.kt$"))
        assertVariant(".foo.ll.kt", ".foo.kt")
        assertVariant("..ll.kt", "..kt")
    }

    @Test
    fun testEmptyParts() {
        assertVariant("foo..ll.kt", "foo..kt")
        assertNotVariant("foo..ll.kt", "foo.ll.kt", "foo.kt")
        assertNotVariant("foo..kt", "foo.kt")
    }

    @Test
    fun testOneRunOfParts() {
        assertVariant("foo.ll.x.reversed.kt", "foo.x.reversed.kt")
        assertVariant("foo.ll.x.reversed.kt", "foo.ll.x.kt")
        assertVariant("foo.ll.x.reversed.kt", "foo.reversed.kt")
        assertNotVariant("foo.ll.x.reversed.kt", "foo.x.kt")
    }

    @Test
    fun testPatternGroup() {
        val canFreezeIde = Pattern.compile("^(.+)\\.kt(\\.can-freeze-ide)?$")
        assertVariant("foo.ll.kt.can-freeze-ide", "foo.kt.can-freeze-ide", pattern = canFreezeIde)
        assertVariant("foo.ll.kt", "foo.kt", pattern = canFreezeIde)
        assertNotVariant("foo.ll.kt.can-freeze-ide", "foo.kt", pattern = canFreezeIde)

        val withoutUnderscorePrefix = Pattern.compile("^([^_](.+))\\.kt$")
        assertVariant("foo.ll.kt", "foo.kt", pattern = withoutUnderscorePrefix)
        assertNotVariant("ab.ll.kt", "ab.kt", pattern = Pattern.compile("^(.{3,})\\.kt$"))

        assertNotVariant("foo.ll.kt", "foo.kt", pattern = Pattern.compile("^.+\\.kt$"))
        assertNotVariant("foo.ll.kt", "foo.kt", pattern = Pattern.compile("^(bar)?.+\\.kt$"))
        assertNotVariant("foo.ll.kt", "foo.kt", pattern = Pattern.compile("^(.+)\\.kts$"))
    }

    @Test
    fun testTextBeforePatternGroup() {
        val modelPattern = Pattern.compile("^model-(.+)\\.xml$")
        assertVariant("model-foo.ll.xml", "model-foo.xml", pattern = modelPattern)
        assertNotVariant("model-.ll.xml", "model-.xml", pattern = modelPattern)
        assertVariant("model-.foo.ll.xml", "model-.foo.xml", pattern = modelPattern)

        val prefixWithDot = Pattern.compile("^a\\.b-(.+)\\.kt$")
        assertVariant("a.b-foo.ll.kt", "a.b-foo.kt", pattern = prefixWithDot)
        assertNotVariant("a.b-foo.kt", "a.kt", "a.foo.kt", pattern = prefixWithDot)

        val optionalPrefixWithDot = Pattern.compile("^(?:x\\.y-)?(.+)\\.kt$")
        assertNotVariant("x.y-foo.kt", "x.kt", "x.foo.kt", pattern = optionalPrefixWithDot)
        assertVariant("x.y-foo.ll.kt", "x.y-foo.kt", pattern = optionalPrefixWithDot)
    }

    @Test
    fun testPatternGroupWithoutExtension() {
        val anyName = Pattern.compile("^(.*)$")
        assertVariant("foo.ll.kt", "foo.kt", pattern = anyName)
        assertNotVariant("foo.kt", "foo", pattern = anyName)
        assertNotVariant("foo.ll", "foo", pattern = anyName)
        assertNotVariant("simple.customext.out", "simple.customext", pattern = anyName)
        assertNotVariant("foo.ll.kt", "foo.ll", "foo", pattern = anyName)
    }

    @Test
    fun testDirectories() {
        val directory = createTempDirectory("testDataVariants").toFile()
        try {
            val variantOfFile = directory.resolve("foo.ll.kt").apply { writeText("") }
            val variantOfDirectory = directory.resolve("bar.ll.kt").apply { writeText("") }
            val directoryNamedAsVariant = directory.resolve("foo.reversed.kt").apply { mkdir() }
            directory.resolve("foo.kt").writeText("")
            directory.resolve("bar.kt").mkdir()

            val entries = directory.listFiles()!!
            val fileNames = testDataFileNames(entries)
            assertEquals(setOf("foo.kt", "foo.ll.kt", "bar.ll.kt"), fileNames)
            assertTrue(isTestDataVariant(variantOfFile, fileNames, KT))
            assertFalse(isTestDataVariant(variantOfDirectory, fileNames, KT))
            assertFalse(isTestDataVariant(directoryNamedAsVariant, fileNames, KT))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun assertVariant(name: String, vararg siblings: String, pattern: Pattern = KT) {
        assertTrue(isTestDataVariant(name, setOf(name, *siblings), pattern), "$name must be a variant among ${siblings.toList()}")
    }

    private fun assertNotVariant(name: String, vararg siblings: String, pattern: Pattern = KT) {
        assertFalse(isTestDataVariant(name, setOf(name, *siblings), pattern), "$name must not be a variant among ${siblings.toList()}")
    }

    private companion object {
        val KT: Pattern = Pattern.compile("^(.+)\\.kt$")
        val KTS: Pattern = Pattern.compile("^(.+)\\.kts$")
        val KT_OR_KTS: Pattern = Pattern.compile("^(.+)\\.(kt|kts)$")
    }
}
