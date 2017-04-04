import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf(1, 2, 3)
    val reversed = original.asReversed()

    reversed.clear()

    assertEquals(emptyList<Int>(), reversed)
    assertEquals(emptyList<Int>(), original)
}
