import kotlin.test.*

fun box() {
    val arr = FloatArray(2)

    val expected: Float = 0.0F
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])
}
