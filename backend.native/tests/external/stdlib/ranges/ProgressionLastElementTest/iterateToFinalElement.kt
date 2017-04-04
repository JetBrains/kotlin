import kotlin.test.*
import kotlin.comparisons.*
import kotlin.test.assertEquals

private val MAX = Int.MAX_VALUE
private val MIN = Int.MIN_VALUE

private val INTERESTING = intArrayOf(MIN, MIN / 2, -239, -23, -1, 0, 1, 42, 239, MAX / 2, MAX)

private fun doTest(start: Int, end: Int, increment: Int, expected: Int) {

    val actualInt = IntProgression.fromClosedRange(start, end, increment).last
    assertEquals(expected, actualInt)

    val actualLong = LongProgression.fromClosedRange(start.toLong(), end.toLong(), increment.toLong()).last
    assertEquals(expected.toLong(), actualLong)
}

fun box() {
    // Small tests
    for (start in -5..4) {
        for (end in -5..4) {
            for (increment in -10..9) {
                // Cut down incorrect test data
                if (increment == 0) continue
                if (increment > 0 != start <= end) continue

                // Iterate over the progression and obtain the expected result
                // println("$start,$end,$increment")
                var x = start
                while (true) {
                    val next = x + increment
                    if (next !in minOf(start, end)..maxOf(start, end)) break
                    x = next
                }

                doTest(start, end, increment, x)
            }
        }
    }
}
