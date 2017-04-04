import kotlin.test.*

data class Value(val value: Int) {
    override fun hashCode(): Int = value
}

fun box() {
    val arr = arrayOf(null, Value(2), arrayOf(Value(3)))
    assertEquals(((1 * 31 + 0) * 31 + 2) * 31 + (1 * 31 + 3), arr.contentDeepHashCode())
}
