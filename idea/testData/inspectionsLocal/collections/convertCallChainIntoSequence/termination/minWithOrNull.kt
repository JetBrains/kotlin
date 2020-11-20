// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.minWithOrNull { _, _ -> 0 }
}