import kotlin.test.*

fun box() {
    val values = arrayOf("ac", "aD", "aba")
    val indices = values.indices.toList().toIntArray()

    assertEquals(listOf(1, 2, 0), indices.sortedBy { values[it] })
}
