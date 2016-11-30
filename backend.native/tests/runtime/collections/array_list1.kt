fun assertTrue(cond: Boolean) {
    if (!cond)
       println("FAIL")
}

fun assertFalse(cond: Boolean) {
    if (cond)
        println("FAIL")
}

fun assertEquals(value1: String, value2: String) {
    if (value1 != value2)
        println("FAIL")
}

fun assertEquals(value1: ArrayList<String>, value2: ArrayList<String>) {
    if (value1 != value2)
        println("FAIL")
}

fun assertEquals(value1: Int, value2: Int) {
    if (value1 != value2)
        println("FAIL")
}

fun testBasic() {
    val a = ArrayList<String>()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)

    assertTrue(a.add("1"))
    assertTrue(a.add("2"))
    assertTrue(a.add("3"))
    assertFalse(a.isEmpty())
    assertEquals(3, a.size)
    assertEquals("1", a[0])
    assertEquals("2", a[1])
    assertEquals("3", a[2])

    a[0] = "11"
    assertEquals("11", a[0])

    assertEquals("11", a.removeAt(0))
    assertEquals(2, a.size)
    assertEquals("2", a[0])
    assertEquals("3", a[1])

    a.add(1, "22")
    assertEquals(3, a.size)
    assertEquals("2", a[0])
    assertEquals("22", a[1])
    assertEquals("3", a[2])

    a.clear()
    assertTrue(a.isEmpty())
    assertEquals(0, a.size)
}

fun makeList123() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("1")
    a.add("2")
    a.add("3")
    return a
}

fun makeList12345() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("1")
    a.add("2")
    a.add("3")
    a.add("4")
    a.add("5")
    return a
}

fun makeList12341() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("1")
    a.add("2")
    a.add("3")
    a.add("4")
    a.add("1")
    return a
}

fun makeList01234() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("0")
    a.add("1")
    a.add("2")
    a.add("3")
    a.add("4")
    return a
}

fun makeList678() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("6")
    a.add("7")
    a.add("8")
    return a
}

fun makeList531() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("5")
    a.add("3")
    a.add("1")
    return a
}

fun makeList135() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("1")
    a.add("3")
    a.add("5")
    return a
}

fun makeList24() : ArrayList<String> {
    val a = ArrayList<String>()
    a.add("2")
    a.add("4")
    return a
}

fun testIterator() {
    val a = makeList123()
    val it = a.iterator()
    assertTrue(it.hasNext())
    assertEquals("1", it.next())
    assertTrue(it.hasNext())
    assertEquals("2", it.next())
    assertTrue(it.hasNext())
    assertEquals("3", it.next())
    assertFalse(it.hasNext())
}

fun testRemove() {
    val a = makeList123()
    assertTrue(a.remove("2"))
    assertEquals(2, a.size)
    assertEquals("1", a[0])
    assertEquals("3", a[1])
    assertFalse(a.remove("2"))
    assertEquals(2, a.size)
    assertEquals("1", a[0])
    assertEquals("3", a[1])
}

fun testRemoveAll() {
    val a = ArrayList(makeList12345())
    assertFalse(a.removeAll(makeList678()))
    assertEquals(makeList12345(), a)
    assertTrue(a.removeAll(makeList531()))
    assertEquals(makeList24(), a)
}

fun testRetainAll() {
    val a = makeList12345()
    assertFalse(a.retainAll(makeList12345()))
    assertEquals(makeList12345(), a)
    assertTrue(a.retainAll(makeList531()))
    assertEquals(makeList135(), a)
}

fun testEquals() {
    val a = makeList123()
    assertTrue(a == makeList123())
    assertFalse(a == makeList135())
    assertFalse(a == makeList24())
}

fun testHashCode() {
    val a = makeList123()
    assertTrue(a.hashCode() == makeList123().hashCode())
}

fun testToString() {
    val a = makeList123()
    assertTrue(a.toString() == makeList123().toString())
}

fun testToString2() {
    val a = makeList123()
    assertEquals(a.toString(), "[1, 2, 3]")
}


fun testSubList() {
    val a0 = makeList01234()
    val a = a0.subList(1, 4)
    assertEquals(3, a.size)
    assertEquals("1", a[0])
    assertEquals("2", a[1])
    assertEquals("3", a[2])
    assertTrue(a == makeList123())
    assertTrue(a.hashCode() == makeList123().hashCode())
    assertTrue(a.toString() == makeList123().toString())
}

fun testResize() {
    val a = ArrayList<String>()
    val n = 10000
    var i = 0
    while (i++ < n)
        assertTrue(a.add(i.toString()))
    assertEquals(n, a.size)
    i = 0
    while (i++ < n)
        assertEquals(i.toString(), a[i - 1])
    a.trimToSize()
    assertEquals(n, a.size)
    i = 0
    while (i++ < n)
        assertEquals(i.toString(), a[i - 1])
}

fun testSubListContains() {
    val a = makeList12345()
    val s = a.subList(1, 3)
    assertTrue(a.contains("1"))
    assertFalse(s.contains("1"))
    assertTrue(a.contains("2"))
    assertTrue(s.contains("2"))
    assertTrue(a.contains("3"))
    assertTrue(s.contains("3"))
    assertTrue(a.contains("4"))
    assertFalse(s.contains("4"))
}

fun testSubListIndexOf() {
    val a = makeList12341()
    val s = a.subList(1, 3)
    assertEquals(0, a.indexOf("1"))
    assertEquals(-1, s.indexOf("1"))
    assertEquals(1, a.indexOf("2"))
    assertEquals(0, s.indexOf("2"))
    assertEquals(2, a.indexOf("3"))
    assertEquals(1, s.indexOf("3"))
    assertEquals(3, a.indexOf("4"))
    assertEquals(-1, s.indexOf("4"))
}

fun testSubListLastIndexOf() {
    val a = makeList12341()
    val s = a.subList(1, 3)
    assertEquals(4, a.lastIndexOf("1"))
    assertEquals(-1, s.lastIndexOf("1"))
    assertEquals(1, a.lastIndexOf("2"))
    assertEquals(0, s.lastIndexOf("2"))
    assertEquals(2, a.lastIndexOf("3"))
    assertEquals(1, s.lastIndexOf("3"))
    assertEquals(3, a.lastIndexOf("4"))
    assertEquals(-1, s.lastIndexOf("4"))
}

fun testIteratorRemove() {
    val a = makeList12345()
    val it = a.iterator()
    var i = 1
    while (it.hasNext()) {
        if (i++ % 2 == 0) {
            it.remove()
        }
        it.next()
    }
    assertEquals(makeList135(), a)
}

fun testIteratorAdd() {
    val a = makeList12345()
    val it = a.listIterator()
    var i = 0
    while (it.hasNext()) {
        val next = it.next()
        if (i++ % 2 == 0)
            it.add("-" + next)
    }
    //assertEquals(listOf("1", "2", "-2", "3", "4", "-4", "5"), a)
}



fun main(args : Array<String>) {
    testBasic()
    testIterator()
    testRemove()
    testRemoveAll()
    // Fails due to unknown virtual method call!
    // testRetainAll()
    testEquals()
    testHashCode()
    testToString()
    testToString2()
    testSubList()
    testResize()
    testSubListContains()
    testSubListIndexOf()
    testSubListLastIndexOf()
    // Fails due to unknown virtual method call!
    // testIteratorAdd()
    // testIteratorRemove()
    println("OK")
}