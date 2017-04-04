import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(Triple(1, "a", 0.07).hashCode(), t.hashCode())
    assertNotEquals(Triple(2, "a", 0.07).hashCode(), t.hashCode())
    assertNotEquals(0, Triple(null, "b", 0.07).hashCode())
    assertNotEquals(0, Triple("b", null, 0.07).hashCode())
    assertNotEquals(0, Triple("b", 1, null).hashCode())
    assertEquals(0, Triple(null, null, null).hashCode())
}
