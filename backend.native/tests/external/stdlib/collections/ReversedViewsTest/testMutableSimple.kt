import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertEquals(listOf(3, 2, 1), mutableListOf(1, 2, 3).asReversed())
    assertEquals(listOf(3, 2, 1), mutableListOf(1, 2, 3).asReversed().toList())
}
