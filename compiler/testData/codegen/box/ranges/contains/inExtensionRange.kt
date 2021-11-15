// WITH_STDLIB
import kotlin.test.*

operator fun Int.rangeTo(right: String): ClosedRange<Int> = this..this + 1
operator fun Long.rangeTo(right: Double): ClosedRange<Long> = this..right.toLong() + 1
operator fun String.rangeTo(right: Int): ClosedRange<String> = this..this

fun box(): String {
    assertTrue(0 !in 1.."a")
    assertTrue(1 in 1.."a")

    assertTrue(0L !in 1L..2.0)
    assertTrue(2L in 1L..3.0)

    assertTrue("a" !in "b"..1)
    assertTrue("a" in "a"..1)

    return "OK"
}
