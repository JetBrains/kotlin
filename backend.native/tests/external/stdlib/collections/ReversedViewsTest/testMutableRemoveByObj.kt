import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf("a", "b", "c")
    val reversed = original.asReversed()

    reversed.remove("c")
    assertEquals(listOf("a", "b"), original)
    assertEquals(listOf("b", "a"), reversed)
}
