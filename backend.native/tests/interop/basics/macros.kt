import kotlin.test.*
import cmacros.*

fun main(args: Array<String>) {
    assertEquals("foo", FOO_STRING)
    assertEquals(0, ZERO)
    assertEquals(1, ONE)
    assertEquals(Long.MAX_VALUE, MAX_LONG)
    assertEquals(42, FOURTY_TWO)

    val seventeen: Long = SEVENTEEN
    assertEquals(17L, seventeen)

    val onePointFive: Float = ONE_POINT_FIVE
    val onePointZero: Double = ONE_POINT_ZERO

    assertEquals(1.5f, onePointFive)
    assertEquals(1.0, onePointZero)
}
