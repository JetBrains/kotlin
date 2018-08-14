// WITH_RUNTIME

fun test(list: List<Int>) {
    val partition: Pair<List<Int>, List<Int>> = list.<caret>filter { it > 1 }.partition { true }
}