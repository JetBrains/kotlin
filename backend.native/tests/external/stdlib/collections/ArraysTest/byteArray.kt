import kotlin.test.*

fun box() {
    val arr = ByteArray(2)

    val expected: Byte = 0
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])
}
