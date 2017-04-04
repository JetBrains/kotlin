import kotlin.test.*

fun box() {
    val arr = LongArray(2)

    val expected: Long = 0
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])
}
