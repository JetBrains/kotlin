// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.maxOrNull()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.maxOrNull()'"
fun getMaxLineWidth(lineCount: Int): Float {
    var max_width = 0.0f
    <caret>for (i in 0..lineCount - 1) {
        val width = getLineWidth(i)
        if (width > max_width)
            max_width = width
    }
    return max_width
}

fun getLineWidth(i: Int): Float = TODO()
