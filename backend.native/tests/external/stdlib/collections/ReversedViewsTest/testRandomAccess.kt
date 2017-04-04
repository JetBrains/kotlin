import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val reversed = listOf(1, 2, 3).asReversed()

    assertEquals(3, reversed[0])
    assertEquals(2, reversed[1])
    assertEquals(1, reversed[2])
}
