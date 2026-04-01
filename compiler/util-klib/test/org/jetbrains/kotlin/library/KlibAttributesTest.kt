/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class KlibAttributesTest {
    @TempDir
    lateinit var tmpDir: File

    @Test
    fun `Public member attributes reading and writing`() {
        val klib = generateMockKlib()

        assertEquals(null, klib.testPublicMemberAnyAttribute)
        klib.testPublicMemberAnyAttribute = ANY_VALUE1
        assertEquals(ANY_VALUE1, klib.testPublicMemberAnyAttribute)
        klib.testPublicMemberAnyAttribute = ANY_VALUE2
        assertEquals(ANY_VALUE2, klib.testPublicMemberAnyAttribute)
        klib.testPublicMemberAnyAttribute = null
        assertEquals(null, klib.testPublicMemberAnyAttribute)

        assertEquals(null, klib.testPublicMemberStringAttribute)
        klib.testPublicMemberStringAttribute = "foo"
        assertEquals("foo", klib.testPublicMemberStringAttribute)
        klib.testPublicMemberStringAttribute = "bar"
        assertEquals("bar", klib.testPublicMemberStringAttribute)
        klib.testPublicMemberStringAttribute = null
        assertEquals(null, klib.testPublicMemberStringAttribute)

        assertFalse(klib.testPublicMemberBooleanAttribute)
        klib.testPublicMemberBooleanAttribute = true
        assertTrue(klib.testPublicMemberBooleanAttribute)
        klib.testPublicMemberBooleanAttribute = true
        assertTrue(klib.testPublicMemberBooleanAttribute)
        klib.testPublicMemberBooleanAttribute = false
        assertFalse(klib.testPublicMemberBooleanAttribute)
    }

    @Test
    fun `Private member attributes reading and writing`() {
        val klib = generateMockKlib()

        assertEquals(null, klib.testPrivateMemberAnyAttribute)
        klib.testPrivateMemberAnyAttribute = ANY_VALUE1
        assertEquals(ANY_VALUE1, klib.testPrivateMemberAnyAttribute)
        klib.testPrivateMemberAnyAttribute = ANY_VALUE2
        assertEquals(ANY_VALUE2, klib.testPrivateMemberAnyAttribute)
        klib.testPrivateMemberAnyAttribute = null
        assertEquals(null, klib.testPrivateMemberAnyAttribute)

        assertEquals(null, klib.testPrivateMemberStringAttribute)
        klib.testPrivateMemberStringAttribute = "foo"
        assertEquals("foo", klib.testPrivateMemberStringAttribute)
        klib.testPrivateMemberStringAttribute = "bar"
        assertEquals("bar", klib.testPrivateMemberStringAttribute)
        klib.testPrivateMemberStringAttribute = null
        assertEquals(null, klib.testPrivateMemberStringAttribute)

        assertFalse(klib.testPrivateMemberBooleanAttribute)
        klib.testPrivateMemberBooleanAttribute = true
        assertTrue(klib.testPrivateMemberBooleanAttribute)
        klib.testPrivateMemberBooleanAttribute = true
        assertTrue(klib.testPrivateMemberBooleanAttribute)
        klib.testPrivateMemberBooleanAttribute = false
        assertFalse(klib.testPrivateMemberBooleanAttribute)
    }

    @Test
    fun `Public top-level attributes reading and writing`() {
        val klib = generateMockKlib()

        assertEquals(null, klib.testPublicTopLevelAnyAttribute)
        klib.testPublicTopLevelAnyAttribute = ANY_VALUE1
        assertEquals(ANY_VALUE1, klib.testPublicTopLevelAnyAttribute)
        klib.testPublicTopLevelAnyAttribute = ANY_VALUE2
        assertEquals(ANY_VALUE2, klib.testPublicTopLevelAnyAttribute)
        klib.testPublicTopLevelAnyAttribute = null
        assertEquals(null, klib.testPublicTopLevelAnyAttribute)

        assertEquals(null, klib.testPublicTopLevelStringAttribute)
        klib.testPublicTopLevelStringAttribute = "foo"
        assertEquals("foo", klib.testPublicTopLevelStringAttribute)
        klib.testPublicTopLevelStringAttribute = "bar"
        assertEquals("bar", klib.testPublicTopLevelStringAttribute)
        klib.testPublicTopLevelStringAttribute = null
        assertEquals(null, klib.testPublicTopLevelStringAttribute)

        assertFalse(klib.testPublicTopLevelBooleanAttribute)
        klib.testPublicTopLevelBooleanAttribute = true
        assertTrue(klib.testPublicTopLevelBooleanAttribute)
        klib.testPublicTopLevelBooleanAttribute = true
        assertTrue(klib.testPublicTopLevelBooleanAttribute)
        klib.testPublicTopLevelBooleanAttribute = false
        assertFalse(klib.testPublicTopLevelBooleanAttribute)
    }

    @Test
    fun `Private top-level attributes reading and writing`() {
        val klib = generateMockKlib()

        assertEquals(null, klib.testPrivateTopLevelAnyAttribute)
        klib.testPrivateTopLevelAnyAttribute = ANY_VALUE1
        assertEquals(ANY_VALUE1, klib.testPrivateTopLevelAnyAttribute)
        klib.testPrivateTopLevelAnyAttribute = ANY_VALUE2
        assertEquals(ANY_VALUE2, klib.testPrivateTopLevelAnyAttribute)
        klib.testPrivateTopLevelAnyAttribute = null
        assertEquals(null, klib.testPrivateTopLevelAnyAttribute)

        assertEquals(null, klib.testPrivateTopLevelStringAttribute)
        klib.testPrivateTopLevelStringAttribute = "foo"
        assertEquals("foo", klib.testPrivateTopLevelStringAttribute)
        klib.testPrivateTopLevelStringAttribute = "bar"
        assertEquals("bar", klib.testPrivateTopLevelStringAttribute)
        klib.testPrivateTopLevelStringAttribute = null
        assertEquals(null, klib.testPrivateTopLevelStringAttribute)

        assertFalse(klib.testPrivateTopLevelBooleanAttribute)
        klib.testPrivateTopLevelBooleanAttribute = true
        assertTrue(klib.testPrivateTopLevelBooleanAttribute)
        klib.testPrivateTopLevelBooleanAttribute = true
        assertTrue(klib.testPrivateTopLevelBooleanAttribute)
        klib.testPrivateTopLevelBooleanAttribute = false
        assertFalse(klib.testPrivateTopLevelBooleanAttribute)
    }

    private fun generateMockKlib(): Klib = KlibLoader {
        libraryPaths(
            KlibMockDSL.mockKlib(tmpDir) {
                manifest(
                    uniqueName = "foo",
                    builtInsPlatform = BuiltInsPlatform.COMMON,
                    versioning = KotlinLibraryVersioning(null, null, null)
                )
            }
        )
    }.load().librariesStdlibFirst[0]

    var Klib.testPublicMemberAnyAttribute: Any? by klibAttribute()
    var Klib.testPublicMemberStringAttribute: String? by klibAttribute()
    var Klib.testPublicMemberBooleanAttribute: Boolean by klibFlag()

    private var Klib.testPrivateMemberAnyAttribute: Any? by klibAttribute()
    private var Klib.testPrivateMemberStringAttribute: String? by klibAttribute()
    private var Klib.testPrivateMemberBooleanAttribute: Boolean by klibFlag()

    companion object {
        private val ANY_VALUE1 = Any()
        private val ANY_VALUE2 = Any()
    }
}

var Klib.testPublicTopLevelAnyAttribute: Any? by klibAttribute()
var Klib.testPublicTopLevelStringAttribute: String? by klibAttribute()
var Klib.testPublicTopLevelBooleanAttribute: Boolean by klibFlag()

private var Klib.testPrivateTopLevelAnyAttribute: Any? by klibAttribute()
private var Klib.testPrivateTopLevelStringAttribute: String? by klibAttribute()
private var Klib.testPrivateTopLevelBooleanAttribute: Boolean by klibFlag()
