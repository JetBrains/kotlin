import kotlin.test.*

fun box() {
    val arr = DoubleArray(2) { it.toDouble() }

    assertEquals(2, arr.size)
    assertEquals(0.toDouble(), arr[0])
    assertEquals(1.toDouble(), arr[1])
}
