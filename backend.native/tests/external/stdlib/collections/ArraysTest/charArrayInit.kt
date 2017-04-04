import kotlin.test.*

fun box() {
    val arr = CharArray(2) { 'a' + it }

    assertEquals(2, arr.size)
    assertEquals('a', arr[0])
    assertEquals('b', arr[1])
}
