// WITH_RUNTIME

fun test() {
    val array: IntArray = intArrayOf(0, 1, 2, 3)
    val sb = StringBuilder()
    array.<caret>map { "$it-$it" }.joinTo(sb)
}