import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(listOf(1, 2), (1 to 2).toList())
    assertEquals(listOf(1, null), (1 to null).toList())
    assertEquals(listOf(1, "2"), (1 to "2").toList())
}
