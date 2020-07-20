// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>filter { it > 1 }.scan(0) { acc, i -> acc + i }
}
