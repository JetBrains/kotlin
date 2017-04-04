import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    val s = hashSetOf(Triple(1, "a", 0.07), Triple(1, "b", 0.07), Triple(1, "a", 0.07))
    assertEquals(2, s.size)
    assertTrue(s.contains(t))
}
