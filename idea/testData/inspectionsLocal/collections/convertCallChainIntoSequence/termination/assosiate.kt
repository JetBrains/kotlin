// WITH_RUNTIME

fun test(list: List<Int>) {
    val associate: Map<Int, Int> = list.<caret>filter { it > 1 }.associate { it to it }
}