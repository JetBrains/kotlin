// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <E> emptyList(): List<E> = TODO()

data class IntervalTree(
    val left: IntervalTree?,
    val right: IntervalTree?,
    val intervals: List<Interval>,
    val median: Float
)

class Interval

fun buildTree(segments: List<Interval>): IntervalTree? = TODO()
fun acquireIntervals(): List<Interval> = TODO()

fun main() {
    buildTree(acquireIntervals())
        ?: <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()
}
