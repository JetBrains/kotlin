// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.maxOfWith({ _, _ -> 0 }) { it }
}