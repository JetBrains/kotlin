// WITH_STDLIB
import kotlin.test.*

const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1
val DOUBLE = Double.MAX_VALUE - 1.0

fun box(): String {
    assertEquals(1073741824, INT_MAX_POWER_OF_TWO)
    assertTrue(DOUBLE > 0.0)

    return "OK"
}
