/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.ResolvedDependencyId
import org.jetbrains.kotlin.utils.ResolvedDependencyVersion
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class ResolvedDependencyIdTest {
    @Test
    fun failOnNoNames1() {
        assertThrows<IllegalStateException> {
            ResolvedDependencyId()
        }
    }

    @Test
    fun failOnNoNames2() {
        assertThrows<IllegalStateException> {
            ResolvedDependencyId(emptyList())
        }
    }

    @Test
    fun namesAreOrdered() {
        val characters: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val uniqueNames: List<String> = List(10) {
            List(20) { characters.random() }.joinToString("")
        }
        val moduleId = ResolvedDependencyId(uniqueNames)
        Assertions.assertEquals(uniqueNames.sorted(), moduleId.uniqueNames.toList())
    }

    @Test
    fun contains() {
        Assertions.assertTrue(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "bar"))
        Assertions.assertTrue(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "bar", "baz"))
        Assertions.assertFalse(ResolvedDependencyId("foo", "bar") in ResolvedDependencyId("foo", "baz"))
    }

    @Test
    fun withVersion() {
        Assertions.assertEquals("foo: 1.0.1", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion("1.0.1")))
        Assertions.assertEquals("foo", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion("")))
        Assertions.assertEquals("foo", ResolvedDependencyId("foo").withVersion(ResolvedDependencyVersion.EMPTY))
    }

    @Test
    fun toStringImplementation() {
        Assertions.assertEquals("/", ResolvedDependencyId.DEFAULT_SOURCE_CODE_MODULE_ID.toString())
        Assertions.assertEquals("foo", ResolvedDependencyId("foo").toString())
        Assertions.assertEquals("bar (foo)", ResolvedDependencyId("foo", "bar").toString())
        Assertions.assertEquals("bar (baz, foo)", ResolvedDependencyId("foo", "bar", "baz").toString())
        Assertions.assertEquals("bar (baz, foo, qux)", ResolvedDependencyId("foo", "bar", "baz", "qux").toString())
    }
}
