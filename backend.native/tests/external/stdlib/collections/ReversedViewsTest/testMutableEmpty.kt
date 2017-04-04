import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertEquals(emptyList<Int>(), mutableListOf<Int>().asReversed())
}
