// WITH_RUNTIME

fun test(list: List<Int>) {
    val indexOf: Int = list.<caret>filter { it > 1 }.indexOf(1)
}