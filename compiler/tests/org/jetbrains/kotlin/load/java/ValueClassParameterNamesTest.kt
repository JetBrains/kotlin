/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ValueClassParameterNamesTest {
    private fun assertRoundTrip(classFqName: String, parameterName: String) {
        val decoded = ValueClassParameterNames.decodeValueClassParameterNameOrNull(ValueClassParameterNames.encode(classFqName, parameterName))
        assertEquals(classFqName, decoded?.classFqName)
        assertEquals(parameterName, decoded?.parameterName)
    }

    @Test
    fun roundTrip() {
        assertRoundTrip("pkg.IC", "x")
        assertRoundTrip("pkg.Outer\$Inner", "x")
        assertRoundTrip("pkg.with-dash and space", "x")
        assertRoundTrip("pkg.\uD835\uDD38", "x")
        assertRoundTrip("", "x")
        assertRoundTrip("pkg.IC", "\$this\$extension")
    }

    @Test
    fun sourceNameStart() {
        val name = ValueClassParameterNames.encode("kotlin.UInt", "iv")
        assertEquals("iv", name.substring(ValueClassParameterNames.sourceNameStart(name)))
        assertEquals("iv\$iv", (name + "\$iv").substring(ValueClassParameterNames.sourceNameStart(name + "\$iv")))
        assertEquals(0, ValueClassParameterNames.sourceNameStart("iv\$iv"))
        assertEquals(0, ValueClassParameterNames.sourceNameStart("\$v\$c\$\$q\$\$iv"))
    }

    @Test
    fun malformedNamesAreNotDecoded() {
        val malformed = listOf(
            "\$v\$c\$\$u-ABB\$\$x",
            "\$v\$c\$\$u+041\$\$x",
            "\$v\$c\$\$u00g1\$\$x",
            "\$v\$c\$\$U7fffffff\$\$x",
            "\$v\$c\$\$Uffffffff\$\$x",
            "\$v\$c\$\$u00",
            "\$v\$c\$\$q\$\$x",
            "\$v\$c\$pkg\$_IC",
            "pkg\$_IC\$\$x",
        )
        for (name in malformed) {
            assertNull(ValueClassParameterNames.decodeValueClassParameterNameOrNull(name), name)
            assertEquals(name, ValueClassParameterNames.originalParameterName(name), name)
        }
    }
}
