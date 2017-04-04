import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(Pair(1, "a").hashCode(), p.hashCode())
    assertNotEquals(Pair(2, "a").hashCode(), p.hashCode())
    assertNotEquals(0, Pair(null, "b").hashCode())
    assertNotEquals(0, Pair("b", null).hashCode())
    assertEquals(0, Pair(null, null).hashCode())
}
