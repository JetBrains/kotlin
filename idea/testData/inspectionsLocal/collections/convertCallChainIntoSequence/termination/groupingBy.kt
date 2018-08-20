// WITH_RUNTIME

fun test(list: List<Int>) {
    val groupingBy: Grouping<Int, Int> = list.<caret>filter { it > 1 }.groupingBy { it }
}