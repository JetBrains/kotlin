// PROBLEM: none
// WITH_RUNTIME

fun test() {
    val array: IntArray = intArrayOf(0, 1, 2, 3)
    array.<caret>map { if (it > 0) it else null }.filterNotNull()
}