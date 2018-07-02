// WITH_RUNTIME

fun test(list: List<Int>) {
    val toSet: Set<Int> = list.<caret>filter { it > 1 }.toSet()
}