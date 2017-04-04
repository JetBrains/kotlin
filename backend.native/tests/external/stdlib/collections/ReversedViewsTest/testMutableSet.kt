import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf(1, 2, 3)
    val reversed = original.asReversed()

    reversed.set(0, 300)
    reversed.set(1, 200)
    reversed.set(2, 100)

    assertEquals(listOf(100, 200, 300), original)
    assertEquals(listOf(300, 200, 100), reversed)
}
