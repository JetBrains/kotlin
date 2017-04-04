import kotlin.test.*

fun box() {
    val arr = DoubleArray(2)

    assertEquals(arr.size, 2)
    assertEquals(0.0, arr[0])
    assertEquals(0.0, arr[1])
}
