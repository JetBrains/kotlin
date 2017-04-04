import kotlin.test.*

fun box() {
    val arr = BooleanArray(2)
    assertEquals(arr.size, 2)
    assertEquals(false, arr[0])
    assertEquals(false, arr[1])
}
