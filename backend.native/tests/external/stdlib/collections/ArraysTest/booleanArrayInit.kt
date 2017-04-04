import kotlin.test.*

fun box() {
    val arr = BooleanArray(2) { it % 2 == 0 }

    assertEquals(2, arr.size)
    assertEquals(true, arr[0])
    assertEquals(false, arr[1])
}
