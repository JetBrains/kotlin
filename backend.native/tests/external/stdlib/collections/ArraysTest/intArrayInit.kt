import kotlin.test.*

fun box() {
    val arr = IntArray(2) { it.toInt() }

    assertEquals(2, arr.size)
    assertEquals(0.toInt(), arr[0])
    assertEquals(1.toInt(), arr[1])
}
