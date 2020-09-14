/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.properties

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvablePropertiesTests {

    @Test
    fun `trivial symbol resolve`() {
        val props = propertiesOf(
            "key1" to "value1",
            "key2" to "\$key1"
        )
        assertEquals("value1", props.resolvablePropertyString("key2"))
    }

    @Test(expected = IllegalStateException::class)
    fun `trivial circular dependency`() {
        val props = propertiesOf(
            "key1" to "\$key2",
            "key2" to "\$key1"
        )
        props.resolvablePropertyString("key2")
    }

    @Test
    fun `list and string should have the same behavior`() {
        val props = propertiesOf(
            "k1" to "v1",
            "k2" to "v2 \$k1"
        )
        assertEquals("v2 v1", props.resolvablePropertyString("k2"))
        assertEquals("v2 v1", props.resolvablePropertyList("k2").joinToString(separator = " "))
    }

    @Test
    fun `list expansion`() {
        val props = propertiesOf(
            "k1" to "v1 v2",
            "k2" to "\$k1 v3",
            "k3" to "\$k2"
        )
        assertEquals(listOf("v1", "v2", "v3"), props.resolvablePropertyList("k3"))
    }

    @Test
    fun `double list expansion`() {
        val props = propertiesOf(
            "k1" to "v1 v2",
            "k2" to "\$k1 \$k1"
        )
        assertEquals(listOf("v1", "v2", "v1", "v2"), props.resolvablePropertyList("k2"))
    }

    @Test(expected = IllegalStateException::class)
    fun `self-reference`() {
        val props = propertiesOf(
            "k1" to "\$k1"
        )
        props.resolvablePropertyString("k1")
    }

    companion object {
        private fun propertiesOf(vararg pairs: Pair<String, Any>): Properties =
            Properties().also {
                it.putAll(mapOf(*pairs))
            }
    }
}