/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.jvm

import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf.StringTableTypes.Record
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf.StringTableTypes.Record.Operation.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolverBase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.util.*

class JvmNameResolverTest : KtUsefulTestCase() {
    private class Context {
        val types = JvmProtoBuf.StringTableTypes.newBuilder()
        val strings = ArrayList<String>()

        fun string(
                string: String?,
                range: Int? = null,
                predefinedIndex: Int? = null,
                internalString: String? = null,
                operation: Record.Operation? = null,
                substringIndex: List<Int>? = null,
                replaceChar: List<Char>? = null
        ) {
            types.addRecord(Record.newBuilder().apply {
                range?.let { setRange(it) }
                predefinedIndex?.let { setPredefinedIndex(it) }
                internalString?.let { setString(it) }
                operation?.let { setOperation(it) }
                substringIndex?.let { addAllSubstringIndex(it) }
                replaceChar?.let { addAllReplaceChar(it.map(Char::code)) }
            }.build())

            string?.let { strings.add(it) }
        }
    }

    private fun create(init: Context.() -> Unit): JvmNameResolver {
        return Context().run {
            init()
            JvmNameResolver(types.build(), strings.toTypedArray())
        }
    }

    private fun str(string: String?, predefinedIndex: Int? = null, operation: Record.Operation? = null): String {
        return create { string(string, null, predefinedIndex, null, operation, null, null) }.getString(0)
    }

    fun testSimpleString() {
        assertEquals("abc", str("abc"))
    }

    fun testSimpleClassId() {
        assertEquals(
                ClassId.topLevel(FqName("foo.bar.Baz")),
                create { string("Lfoo/bar/Baz;", operation = DESC_TO_CLASS_ID) }.getClassId(0)
        )
    }

    fun testBasicOperations() {
        assertEquals("java/util/Map.Entry", str("Ljava/util/Map\$Entry;", operation = DESC_TO_CLASS_ID))
        assertEquals("java/util/Map.Entry", str("java/util/Map\$Entry", operation = INTERNAL_TO_CLASS_ID))
    }

    fun testPredefined() {
        for ((index, predefined) in JvmNameResolverBase.PREDEFINED_STRINGS.withIndex()) {
            assertEquals("Predefined string failed: $predefined (index $index)", predefined, str("ignored", predefinedIndex = index))
        }
    }

    fun testNotExistingPredefinedString() {
        assertEquals("not-ignored", str("not-ignored", predefinedIndex = 123456789))
    }

    fun testOperationOnBadString() {
        assertEquals("X", str("X", operation = DESC_TO_CLASS_ID))
        assertEquals("", str("", operation = DESC_TO_CLASS_ID))
    }

    fun testSubstring() {
        val n = create {
            string("kotlin", substringIndex = listOf(0, 6))
            string("kotlin", substringIndex = listOf(1, 4))
            string("kotlin", substringIndex = listOf(6, 6))

            // Invalid operations
            string("kotlin", substringIndex = listOf(7, 5))
            string("kotlin", substringIndex = listOf(0, -2))
            string("kotlin", substringIndex = listOf(3, 1))
        }

        assertEquals("kotlin", n.getString(0))
        assertEquals("otl", n.getString(1))
        assertEquals("", n.getString(2))

        // All invalid operations should be ignored
        (3..5).forEach { assertEquals("kotlin", n.getString(it)) }
    }

    fun testSubstringHappensAfterOperation() {
        assertEquals("tl", create {
            string("kotlin", substringIndex = listOf(1, 5), operation = DESC_TO_CLASS_ID)
        }.getString(0))
    }

    fun testReplaceAll() {
        val n = create {
            string("kotlin", replaceChar = listOf('k', 'm'))
            string("java", replaceChar = listOf('a', 'o', 'a', 'b', 'c', 'd')) // All chars after the first two are ignored

            // Invalid operations
            string("kotlin", replaceChar = listOf())
            string("kotlin", replaceChar = listOf('k'))
        }

        assertEquals("motlin", n.getString(0))
        assertEquals("jovo", n.getString(1))

        // All invalid operations should be ignored
        (2..3).forEach { assertEquals("kotlin", n.getString(it)) }
    }

    fun testRange() {
        val n = create {
            string("a\$b\$c", operation = INTERNAL_TO_CLASS_ID, range = 2)
            string("d\$e\$f", operation = NONE, range = 2)
            string("abc")
            string("def")
        }

        assertEquals("d.e.f", n.getString(1))
        assertEquals("def", n.getString(3))
    }

    fun testRangeWithDifferentOperations() {
        val n = create {
            string("a\$b\$c", operation = INTERNAL_TO_CLASS_ID, range = 2)
            string("d\$e\$f", operation = NONE, substringIndex = listOf(2, 5))
        }

        assertEquals("a.b.c", n.getString(0))
        assertEquals("d.e.f", n.getString(1))
    }

    fun testString() {
        val n = create {
            string("java", internalString = "kotlin", range = 5)
        }

        (0..4).forEach { assertEquals("kotlin", n.getString(it)) }
    }
}
