// !WITH_NEW_INFERENCE
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
        ?: <!OI;TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!><!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>emptyList<!>()<!>
}
