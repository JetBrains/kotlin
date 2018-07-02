// WITH_RUNTIME

fun test(list: List<Int>) {
    val fold: Int = list.<caret>filter { it > 1 }.fold(0) { acc, i -> acc + i }
}