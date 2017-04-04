import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertEquals(listOf(1, 2, 3), mutableListOf(1, 2, 3).asReversed().asReversed())
    assertEquals(listOf(2, 3), mutableListOf(1, 2, 3, 4).asReversed().subList(1, 3).asReversed())
}
