/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.NO_NAME_PROVIDED
import org.jetbrains.kotlin.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.junit.Test
import kotlin.test.assertEquals

class RenderingUtilsTest {
    @Test
    fun testRender() {
        val normalIdentifier = Name.identifier("normal")
        assertEquals("normal", normalIdentifier.render(stipSpecialMarkers = true))
        assertEquals("normal", normalIdentifier.render(stipSpecialMarkers = false))

        val escapedIdentifier = Name.identifier("`escaped`")
        assertEquals("``escaped``", escapedIdentifier.render(stipSpecialMarkers = true))
        assertEquals("``escaped``", escapedIdentifier.render(stipSpecialMarkers = false))

        assertEquals("no name provided", NO_NAME_PROVIDED.render(stipSpecialMarkers = true))
        assertEquals("`<no name provided>`", NO_NAME_PROVIDED.render(stipSpecialMarkers = false))

        assertEquals("unused var", UNDERSCORE_FOR_UNUSED_VAR.render(stipSpecialMarkers = true))
        assertEquals("`<unused var>`", UNDERSCORE_FOR_UNUSED_VAR.render(stipSpecialMarkers = false))

        // Check a keyword
        val keywordIdentifier = Name.identifier("return")
        assertEquals("`return`", keywordIdentifier.render(stipSpecialMarkers = true))
        assertEquals("`return`", keywordIdentifier.render(stipSpecialMarkers = false))
    }
}