import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val success = try {
        listOf(1, 2, 3).asReversed().get(3)
        true
    } catch (expected: IndexOutOfBoundsException) {
        false
    }

    assertFalse(success)
}
