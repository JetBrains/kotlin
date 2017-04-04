import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(Pair(1, "a"), p)
    assertNotEquals(Pair(2, "a"), p)
    assertNotEquals(Pair(1, "b"), p)
    assertTrue(!p.equals(null))
    assertNotEquals("", (p as Any))
}
