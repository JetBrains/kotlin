/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.ResolvedDependencyId
import org.jetbrains.kotlin.utils.ResolvedDependencyVersion
import org.junit.Assert.*
import org.junit.Test

class ResolvedDependencyIdTest {
    @Test(expected = IllegalStateException::class)
    fun failOnNoNames1() {
        ResolvedDependencyId()
    }

    @Test(expected = IllegalStateException::class)
    fun failOnNoNames2() {
        ResolvedDependencyId(emptyList())
    }

    @Test
    fun namesAreOrdered() {
        val characters: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val uniqueNames: List<String> = List(10) {
            List(20) { characters.random() }.joinToString("")
        }
        val moduleId = ResolvedDependencyId(uniqueNames)
        assertEquals(uniqueNames.sorted(), moduleId.uniqueNames.toList())
    }

    @Test
    fun contains() {
        assertTrue(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "bar"))
        assertTrue(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "bar", "baz"))
        assertFalse(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "baz"))
    }

    @Test
    fun withVersion() {
        assertEquals("foo: 1.0.1", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion("1.0.1")))
        assertEquals("foo", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion("")))
        assertEquals("foo", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion.EMPTY))
    }

    @Test
    fun toStringImplementation() {
        assertEquals("/", ResolvedDependencyId.DEFAULT_SOURCE_CODE_MODULE_ID.toString())
        assertEquals("foo", ResolvedDependencyId("foo").toString())
        assertEquals("bar (foo)", ResolvedDependencyId("foo", "bar").toString())
        assertEquals("bar (baz, foo)", ResolvedDependencyId("foo", "bar", "baz").toString())
        assertEquals("bar (baz, foo, qux)", ResolvedDependencyId("foo", "bar", "baz", "qux").toString())
    }
}
