import kotlin.test.*
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val p = Pair(1, "a")
val t = Triple(1, "a", 0.07)
fun box() {
    assertEquals(listOf(1, 2, 3), (Triple(1, 2, 3)).toList())
    assertEquals(listOf(1, null, 3), (Triple(1, null, 3)).toList())
    assertEquals(listOf(1, 2, "3"), (Triple(1, 2, "3")).toList())
}
