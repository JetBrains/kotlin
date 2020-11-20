// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.scanIndexed(0) { _, acc, i -> acc + i }
}