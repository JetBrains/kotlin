import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertTrue { 1 in listOf(1, 2, 3).asReversed() }
    assertTrue { 1 in mutableListOf(1, 2, 3).asReversed() }
}
