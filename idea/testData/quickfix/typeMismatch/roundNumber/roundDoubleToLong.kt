// "Round using roundToLong()" "true"
// WITH_RUNTIME
fun test(d: Double) {
    bar(d<caret>)
}

fun bar(x: Long) {}