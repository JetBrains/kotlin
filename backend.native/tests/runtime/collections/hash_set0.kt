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
    val a = HashSet<String>()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)

    assertTrue(a.add("1"))
    assertTrue(a.add("2"))
    assertTrue(a.add("3"))
    assertFalse(a.isEmpty())
    assertEquals(3, a.size)
    assertTrue(a.contains("1"))
    assertTrue(a.contains("2"))
    assertTrue(a.contains("3"))
    assertFalse(a.contains("4"))

    assertTrue(a.remove("1"))
    assertEquals(2, a.size)
    assertFalse(a.contains("1"))
    assertTrue(a.contains("2"))
    assertTrue(a.contains("3"))
    assertFalse(a.contains("4"))

    assertTrue(a.add("4"))
    assertEquals(3, a.size)
    assertFalse(a.contains("1"))
    assertTrue(a.contains("2"))
    assertTrue(a.contains("3"))
    assertTrue(a.contains("4"))

    assertFalse(a.add("4"))
    assertEquals(3, a.size)
    assertFalse(a.contains("1"))
    assertTrue(a.contains("2"))
    assertTrue(a.contains("3"))
    assertTrue(a.contains("4"))

    a.clear()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)
    assertFalse(a.contains("1"))
    assertFalse(a.contains("2"))
    assertFalse(a.contains("3"))
    assertFalse(a.contains("4"))
}

fun testIterator() {
    val s = HashSet(listOf("1", "2", "3"))
    val it = s.iterator()
    assertTrue(it.hasNext())
    assertEquals("1", it.next())
    assertTrue(it.hasNext())
    assertEquals("2", it.next())
    assertTrue(it.hasNext())
    assertEquals("3", it.next())
    assertFalse(it.hasNext())
}

fun testEquals() {
    val s = HashSet(listOf("1", "2", "3"))
    assertTrue(s == setOf("1", "2", "3"))
    assertTrue(s == setOf("2", "3", "1")) // order does not matter
    assertFalse(s == setOf("1", "2", "4"))
    assertFalse(s == setOf("1", "2"))
}

fun testHashCode() {
    val s = HashSet(listOf("1", "2", "3"))
    assertTrue(s.hashCode() == setOf("1", "2", "3").hashCode())
}

fun testToString() {
    val s = HashSet(listOf("1", "2", "3"))
    assertTrue(s.toString() == setOf("1", "2", "3").toString())
}

fun testContainsAll() {
    val s = HashSet(listOf("1", "2", "3", "4", "5"))
    assertFalse(s.containsAll(listOf("6", "7", "8")))
    assertFalse(s.containsAll(listOf("5", "6", "7")))
    assertFalse(s.containsAll(listOf("4", "5", "6")))
    assertTrue(s.containsAll(listOf("3", "4", "5")))
    assertTrue(s.containsAll(listOf("2", "3", "4")))
}

fun testRemoveAll() {
    val s = HashSet(listOf("1", "2", "3", "4", "5", "1"))
    assertFalse(s.removeAll(listOf("6", "7", "8")))
    assertEquals(setOf("1", "2", "3", "4", "5", "1"), s)
    assertTrue(s.removeAll(listOf("5", "3", "1")))
    assertEquals(setOf("2", "4"), s)
}

fun testRetainAll() {
    val s = HashSet(listOf("1", "2", "3", "4", "5"))
    assertFalse(s.retainAll(listOf("1", "2", "3", "4", "5")))
    assertEquals(setOf("1", "2", "3", "4", "5"), s)
    assertTrue(s.retainAll(listOf("5", "3", "1")))
    assertEquals(setOf("1", "3", "5"), s)
}

fun main(args : Array<String>) {
    testBasic()
    testIterator()
    testEquals()
    testHashCode()
    testToString()
    testContainsAll()
    testRemoveAll()
    testRetainAll()
    println("OK")
}