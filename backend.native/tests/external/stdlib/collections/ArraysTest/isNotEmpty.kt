import kotlin.test.*

fun box() {
    assertFalse(intArrayOf().isNotEmpty())
    assertTrue(intArrayOf(1).isNotEmpty())
}
