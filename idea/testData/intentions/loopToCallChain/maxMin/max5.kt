// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.maxOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.maxOrNull()'"
fun getMaxLineWidth(count: Int): Double {
    var max = 0.0
    <caret>for (i in 0..count-1) {
        max = Math.max(max, getLineWidth(i))
    }
    return max
}

fun getLineWidth(i: Int): Double = TODO()
