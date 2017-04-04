import kotlin.test.*

fun box() {
    val arr = ShortArray(2)

    val expected: Short = 0
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])
}
