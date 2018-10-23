// WITH_RUNTIME
// IS_APPLICABLE: false

fun test() {
    (1 to 2).let<caret> { (i, j) -> i.toLong() }
}