import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    assertEquals("[null]", listOf<String?>(null).asReversed().toString())
}
