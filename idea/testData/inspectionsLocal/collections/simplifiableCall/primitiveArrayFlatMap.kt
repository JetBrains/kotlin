// PROBLEM: none
// WITH_RUNTIME
// DISABLE-ERRORS
fun test() {
    intArrayOf(1, 2).flatMap<caret> { it }
}