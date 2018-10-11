// WITH_RUNTIME

fun test(list: List<Int>) {
    val sumBy: Int = list.<caret>filter { it > 1 }.sumBy { it }
}