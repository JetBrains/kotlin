import kotlin.test.*


fun box() {
    val m1 = mapOf("a" to 1, "b" to null)
    val list = m1.mapNotNull { it.value?.let { v -> "${it.key}$v" } }
    assertEquals(listOf("a1"), list)
}
