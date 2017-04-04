import kotlin.test.*


fun box() {
    val map = mapOf("a" to 1, "b" to 2)
    assertTrue("a" in map)
    assertTrue("c" !in map)
}
