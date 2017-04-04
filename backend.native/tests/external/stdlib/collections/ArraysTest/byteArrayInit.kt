import kotlin.test.*

fun box() {
    val arr = ByteArray(2) { it.toByte() }

    assertEquals(2, arr.size)
    assertEquals(0.toByte(), arr[0])
    assertEquals(1.toByte(), arr[1])
}
