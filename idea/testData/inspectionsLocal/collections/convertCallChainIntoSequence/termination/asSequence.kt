// WITH_RUNTIME

fun test(list: List<Int>) {
    val asSequence: Sequence<Int> = list.<caret>filter { it > 1 }.asSequence()
}