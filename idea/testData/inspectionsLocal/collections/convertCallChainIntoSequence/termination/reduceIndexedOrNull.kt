// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.reduceIndexedOrNull { _, acc, i -> acc + i }
}