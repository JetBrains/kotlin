import kotlin.test.*

fun box() {
    // a[b[a, b]]
    val b = arrayOfNulls<Any>(2)
    val a = arrayOf(b)
    b[0] = a
    b[1] = b
    a.toString()
    assertTrue(true, "toString does not cycle")
    a.contentToString()
    assertTrue(true, "contentToString does not cycle")
    val result = a.contentDeepToString()
    assertEquals("[[[...], [...]]]", result)
}
