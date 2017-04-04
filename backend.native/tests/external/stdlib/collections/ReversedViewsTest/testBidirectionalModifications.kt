import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf(1, 2, 3, 4)
    val reversed = original.asReversed()

    original.removeAt(3)
    assertEquals(listOf(1, 2, 3), original)
    assertEquals(listOf(3, 2, 1), reversed)

    reversed.removeAt(2)
    assertEquals(listOf(2, 3), original)
    assertEquals(listOf(3, 2), reversed)
}
