import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf(1, 2, 3)
    val reversed = original.asReversed()

    reversed.add(0) // add zero at end of reversed
    assertEquals(listOf(3, 2, 1, 0), reversed)
    assertEquals(listOf(0, 1, 2, 3), original)

    reversed.add(0, 4) // add four at position 0
    assertEquals(listOf(4, 3, 2, 1, 0), reversed)
    assertEquals(listOf(0, 1, 2, 3, 4), original)
}
