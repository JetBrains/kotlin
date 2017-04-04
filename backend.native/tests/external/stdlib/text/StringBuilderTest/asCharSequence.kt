import kotlin.test.*


fun box() {
    val original = "Some test string"
    val sb = StringBuilder(original)
    val result = sb.toString()
    val cs = sb as CharSequence

    assertEquals(result.length, cs.length)
    assertEquals(result.length, sb.length)
    for (index in result.indices) {
        assertEquals(result[index], sb[index])
        assertEquals(result[index], cs[index])
    }
    assertEquals(result.substring(2, 6), cs.subSequence(2, 6).toString())
}
