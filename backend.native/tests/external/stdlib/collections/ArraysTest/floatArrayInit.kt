import kotlin.test.*

fun box() {
    val arr = FloatArray(2) { it.toFloat() }

    assertEquals(2, arr.size)
    assertEquals(0.toFloat(), arr[0])
    assertEquals(1.toFloat(), arr[1])
}
