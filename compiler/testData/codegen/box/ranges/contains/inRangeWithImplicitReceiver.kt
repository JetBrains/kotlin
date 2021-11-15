// WITH_STDLIB
import kotlin.test.*

fun Long.inLongs(l: Long, r: Long): Boolean {
    return this in l..r
}

fun Double.inDoubles(l: Double, r: Double): Boolean {
    return this in l..r
}

fun box(): String {
    assertTrue(2L.inLongs(1L, 3L))
    assertTrue(!2L.inLongs(0L, 1L))

    assertTrue(2.0.inDoubles(1.0, 3.0))
    assertTrue(!2.0.inDoubles(0.0, 1.0))

    return "OK"
}
