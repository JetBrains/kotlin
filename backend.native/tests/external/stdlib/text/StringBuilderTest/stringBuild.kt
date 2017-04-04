import kotlin.test.*


fun box() {
    val s = buildString {
        append("a")
        append(true)
    }
    assertEquals("atrue", s)
}
