import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(1, p.first)
    assertEquals("a", p.second)
}
