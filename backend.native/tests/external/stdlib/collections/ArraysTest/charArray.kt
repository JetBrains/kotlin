import kotlin.test.*

fun box() {
    val arr = CharArray(2)

    val expected: Char = '\u0000'
    assertEquals(arr.size, 2)
    assertEquals(expected, arr[0])
    assertEquals(expected, arr[1])
}
