import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val reversed = (1..10).toList().asReversed()
    assertEquals(listOf(9, 8, 7), reversed.subList(1, 4))
}
