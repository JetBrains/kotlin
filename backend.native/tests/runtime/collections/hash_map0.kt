/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.hash_map0

import kotlin.test.*

fun assertTrue(cond: Boolean) {
    if (!cond)
        println("FAIL")
}

fun assertFalse(cond: Boolean) {
    if (cond)
        println("FAIL")
}

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        println("FAIL")
}

fun assertNotEquals(value1: Any?, value2: Any?) {
    if (value1 == value2)
        println("FAIL")
}

fun assertEquals(value1: Int, value2: Int) {
    if (value1 != value2)
        println("FAIL")
}

fun testBasic() {
    val m = HashMap<String, String>()
    assertTrue(m.isEmpty())
    assertEquals(0, m.size)

    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))

    assertEquals(null, m.put("1", "a"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertFalse(m.isEmpty())
    assertEquals(1, m.size)

    assertFalse(m.containsKey("2"))
    assertFalse(m.containsValue("b"))
    assertEquals(null, m.get("2"))

    assertEquals(null, m.put("2", "b"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("b"))
    assertEquals("b", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(2, m.size)

    assertEquals("b", m.put("2", "bb"))
    assertTrue(m.containsKey("1"))
    assertTrue(m.containsValue("a"))
    assertEquals("a", m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("a"))
    assertTrue(m.containsValue("bb"))
    assertEquals("bb", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(2, m.size)

    assertEquals("a", m.remove("1"))
    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))
    assertTrue(m.containsKey("2"))
    assertTrue(m.containsValue("bb"))
    assertEquals("bb", m.get("2"))
    assertFalse(m.isEmpty())
    assertEquals(1, m.size)

    assertEquals("bb", m.remove("2"))
    assertFalse(m.containsKey("1"))
    assertFalse(m.containsValue("a"))
    assertEquals(null, m.get("1"))
    assertFalse(m.containsKey("2"))
    assertFalse(m.containsValue("bb"))
    assertEquals(null, m.get("2"))
    assertTrue(m.isEmpty())
    assertEquals(0, m.size)
}

fun testRehashAndCompact() {
    val m = HashMap<String, String>()
    for (repeat in 1..10) {
        val n = when (repeat) {
            1 -> 1000
            2 -> 10000
            3 -> 10
            else -> 100000
        }
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
            assertEquals(null, m.put(i.toString(), "val$i"))
            assertTrue(m.containsKey(i.toString()))
            assertEquals(i, m.size)
        }
        for (i in 1..n) {
            assertTrue(m.containsKey(i.toString()))
        }
        for (i in 1..n) {
            assertEquals("val$i", m.remove(i.toString()))
            assertFalse(m.containsKey(i.toString()))
            assertEquals(n - i, m.size)
        }
        assertTrue(m.isEmpty())
    }
}

fun testClear() {
    val m = HashMap<String, String>()
    for (repeat in 1..10) {
        val n = when (repeat) {
            1 -> 1000
            2 -> 10000
            3 -> 10
            else -> 100000
        }
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
            assertEquals(null, m.put(i.toString(), "val$i"))
            assertTrue(m.containsKey(i.toString()))
            assertEquals(i, m.size)
        }
        for (i in 1..n) {
            assertTrue(m.containsKey(i.toString()))
        }
        m.clear()
        assertEquals(0, m.size)
        for (i in 1..n) {
            assertFalse(m.containsKey(i.toString()))
        }
    }
}
fun testEquals() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertTrue(m == expected)
    assertTrue(m == mapOf("b" to "2", "c" to "3", "a" to "1"))  // order does not matter
    assertFalse(m == mapOf("a" to "1", "b" to "2", "c" to "4"))
    assertFalse(m == mapOf("a" to "1", "b" to "2", "c" to "5"))
    assertFalse(m == mapOf("a" to "1", "b" to "2"))
    assertEquals(m.keys, expected.keys)
    assertEquals(m.values, expected.values)
    assertEquals(m.entries, expected.entries)
}

fun testHashCode() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertEquals(expected.hashCode(), m.hashCode())
    assertEquals(expected.entries.hashCode(), m.entries.hashCode())
    assertEquals(expected.keys.hashCode(), m.keys.hashCode())
    assertEquals(listOf("1", "2", "3").hashCode(), m.values.hashCode())
}

fun testToString() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertEquals(expected.toString(), m.toString())
    assertEquals(expected.entries.toString(), m.entries.toString())
    assertEquals(expected.keys.toString(), m.keys.toString())
    assertEquals(expected.values.toString(), m.values.toString())
}

fun testPutEntry() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    val e = expected.entries.iterator().next() as MutableMap.MutableEntry<String, String>
    assertTrue(m.entries.contains(e))
    assertTrue(m.entries.remove(e))
    assertTrue(mapOf("b" to "2", "c" to "3") == m)
    assertTrue(m.entries.add(e))
    assertTrue(expected == m)
    assertFalse(m.entries.add(e))
    assertTrue(expected == m)
}

fun testRemoveAllEntries() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertFalse(m.entries.removeAll(mapOf("a" to "2", "b" to "3", "c" to "4").entries))
    assertEquals(expected, m)
    assertTrue(m.entries.removeAll(mapOf("b" to "22", "c" to "3", "d" to "4").entries))
    assertNotEquals(expected, m)
    assertEquals(mapOf("a" to "1", "b" to "2"), m)
}

fun testRetainAllEntries() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertFalse(m.entries.retainAll(expected.entries))
    assertEquals(expected, m)
    assertTrue(m.entries.retainAll(mapOf("b" to "22", "c" to "3", "d" to "4").entries))
    assertEquals(mapOf("c" to "3"), m)
}

fun testContainsAllValues() {
    val m = HashMap(mapOf("a" to "1", "b" to "2", "c" to "3"))
    assertTrue(m.values.containsAll(listOf("1", "2")))
    assertTrue(m.values.containsAll(listOf("1", "2", "3")))
    assertFalse(m.values.containsAll(listOf("1", "2", "3", "4")))
    assertFalse(m.values.containsAll(listOf("2", "3", "4")))
}

fun testRemoveValue() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertFalse(m.values.remove("b"))
    assertEquals(expected, m)
    assertTrue(m.values.remove("2"))
    assertEquals(mapOf("a" to "1", "c" to "3"), m)
}

fun testRemoveAllValues() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertFalse(m.values.removeAll(listOf("b", "c")))
    assertEquals(expected, m)
    assertTrue(m.values.removeAll(listOf("b", "3")))
    assertEquals(mapOf("a" to "1", "b" to "2"), m)
}

fun testRetainAllValues() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    assertFalse(m.values.retainAll(listOf("1", "2", "3")))
    assertEquals(expected, m)
    assertTrue(m.values.retainAll(listOf("1", "2", "c")))
    assertEquals(mapOf("a" to "1", "b" to "2"), m)
}

fun testEntriesIteratorSet() {
    val expected = mapOf("a" to "1", "b" to "2", "c" to "3")
    val m = HashMap(expected)
    val it = m.iterator()
    while (it.hasNext()) {
        val entry = it.next()
        entry.setValue(entry.value + "!")
    }
    assertNotEquals(expected, m)
    assertEquals(mapOf("a" to "1!", "b" to "2!", "c" to "3!"), m)
}

@Test fun runTest() {
    testBasic()
    testRehashAndCompact()
    testClear()
    testEquals()
    testHashCode()
    testToString()
    testPutEntry()
    testRemoveAllEntries()
    testRetainAllEntries()
    testContainsAllValues()
    testRemoveValue()
    testRemoveAllValues()
    testRetainAllValues()
    testEntriesIteratorSet()
    //testDegenerateKeys()
    println("OK")
}