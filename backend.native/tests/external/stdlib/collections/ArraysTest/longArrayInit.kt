import kotlin.test.*

fun box() {
    val arr = LongArray(2) { it.toLong() }

    assertEquals(2, arr.size)
    assertEquals(0.toLong(), arr[0])
    assertEquals(1.toLong(), arr[1])
}
