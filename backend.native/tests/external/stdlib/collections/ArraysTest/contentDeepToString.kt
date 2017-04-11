import kotlin.test.*

fun box() {
    // Don't run this test unless primitive array `is` checks are supported (KT-17137)
    if ((intArrayOf() as Any) is Array<*>) {
        assertTrue(true)
        return
    }

    val arr = arrayOf(null, "aa", 1, null, charArrayOf('d'), arrayOf<Any>())
    arr[0] = arr
    assertEquals("[[...], aa, 1, null, [d], []]", arr.contentDeepToString())
}
