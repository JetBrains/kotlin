// "Round using roundToLong()" "true"
// WITH_RUNTIME
fun test(f: Float) {
    bar(f<caret>)
}

fun bar(x: Long) {}