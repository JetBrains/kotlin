// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.maxOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.maxOrNull()'"
fun getMaxLineWidth(list: List<Double>): Double {
    var max = 0.0
    <caret>for ((i, item) in list.withIndex()) {
        max = Math.max(max, item * i)
    }
    return max
}
