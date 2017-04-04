import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val t = Triple(1, "a", 0.07)
fun box() {
    val (a, b, c) = t
    assertEquals(1, a)
    assertEquals("a", b)
    assertEquals(0.07, c)
}
