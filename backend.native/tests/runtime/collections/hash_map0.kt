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
// 'to' not yet working.
/*
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
} */

fun main(args : Array<String>) {
    testBasic()
    testRehashAndCompact()
    testClear()
    //testEquals()
    println("OK")
}