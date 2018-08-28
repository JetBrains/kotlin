// WITH_RUNTIME

fun test(list: List<Int>) {
    val toList: List<Int> = list.<caret>filter { it > 1 }.toList()
}