import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.*


fun box() {
    val original = mutableListOf("a", "b", "c")
    val reversed = original.asReversed()

    reversed.removeAt(0) // remove c
    assertEquals(listOf("a", "b"), original)
    assertEquals(listOf("b", "a"), reversed)

    reversed.removeAt(1) // remove a
    assertEquals(listOf("b"), original)

    reversed.removeAt(0) // remove remaining b
    assertEquals(emptyList<String>(), original)
}
