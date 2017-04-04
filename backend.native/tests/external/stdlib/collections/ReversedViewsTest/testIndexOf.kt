import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertEquals(2, listOf(1, 2, 3).asReversed().indexOf(1))
    assertEquals(2, mutableListOf(1, 2, 3).asReversed().indexOf(1))
}
