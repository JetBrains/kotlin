import kotlin.test.*



fun box() {
    val s: String? = "hey"
    val ns: String? = null

    assertEquals("hey", s.orEmpty())
    assertEquals("", ns.orEmpty())
}
