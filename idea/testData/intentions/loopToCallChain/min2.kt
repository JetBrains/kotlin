// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.min()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.min()'"
fun getMinLineWidth(lineCount: Int): Double {
    var min_width = Double.MAX_VALUE
    <caret>for (i in 0..lineCount - 1) {
        val width = getLineWidth(i)
        if (min_width > width) {
            min_width = width
        }
    }
    return min_width
}

fun getLineWidth(i: Int): Double = TODO()
