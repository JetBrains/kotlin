import kotlin.test.*

fun box() {
    val arr = ShortArray(2) { it.toShort() }

    assertEquals(2, arr.size)
    assertEquals(0.toShort(), arr[0])
    assertEquals(1.toShort(), arr[1])
}
