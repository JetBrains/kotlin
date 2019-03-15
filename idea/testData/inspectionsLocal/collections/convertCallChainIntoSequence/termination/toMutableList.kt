// WITH_RUNTIME

fun test(list: List<Int>) {
    val toMutableList = list.<caret>filter { it > 1 }.toMutableList()
}