import kotlin.test.*

data class Value(val value: Int) {
    override fun hashCode(): Int = value
}

fun box() {
    val arr = arrayOf("a", 1, null, Value(5))
    assertEquals(listOf(*arr).hashCode(), arr.contentHashCode())
    assertEquals((1 * 31 + 2) * 31 + 3, arrayOf(Value(2), Value(3)).contentHashCode())
}
