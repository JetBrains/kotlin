import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    val s = hashSetOf(Pair(1, "a"), Pair(1, "b"), Pair(1, "a"))
    assertEquals(2, s.size)
    assertTrue(s.contains(p))
}
