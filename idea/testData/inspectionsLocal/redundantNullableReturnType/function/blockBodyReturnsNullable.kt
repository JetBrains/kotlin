// PROBLEM: none
// WITH_RUNTIME
fun foo(xs: List<Int>, b: Boolean): Int?<caret> {
    if (b) {
        return xs.first()
    } else {
        return xs.lastOrNull()
    }
}