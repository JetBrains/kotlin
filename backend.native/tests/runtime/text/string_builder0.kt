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
        println("FAIL: '" + value1 + "' != '" + value2 + "'")
}

fun assertEquals(value1: Int, value2: Int) {
    if (value1 != value2)
        println("FAIL" + value1.toString() + " != " + value2.toString())
}

fun testBasic() {
    val sb = StringBuilder()
    assertEquals(0, sb.length)
    assertEquals("", sb.toString())
    sb.append(1)
    assertEquals(1, sb.length)
    assertEquals("1", sb.toString())
    sb.append(", ")
    assertEquals(3, sb.length)
    assertEquals("1, ", sb.toString())
    sb.append(true)
    assertEquals(7, sb.length)
    assertEquals("1, true", sb.toString())
    sb.append(12345678L)
    assertEquals(15, sb.length)
    assertEquals("1, true12345678", sb.toString())

    sb.length = 0
    assertEquals(0, sb.length)
    assertEquals("", sb.toString())
}

fun main(args : Array<String>) {
    testBasic()
    println("OK")
}