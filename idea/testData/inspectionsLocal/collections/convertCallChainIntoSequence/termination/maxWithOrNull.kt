// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.maxWithOrNull { _, _ -> 0 }
}