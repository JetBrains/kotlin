import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(Triple(1, "a", 0.07), t)
    assertNotEquals(Triple(2, "a", 0.07), t)
    assertNotEquals(Triple(1, "b", 0.07), t)
    assertNotEquals(Triple(1, "a", 0.1), t)
    assertTrue(!t.equals(null))
    assertNotEquals("", (t as Any))
}
