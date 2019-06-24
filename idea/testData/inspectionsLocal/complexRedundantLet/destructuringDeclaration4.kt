// WITH_RUNTIME
// PROBLEM: none

fun test(k: Int) {
    (1 to 2).let<caret> { (i, j) -> i + k }
}