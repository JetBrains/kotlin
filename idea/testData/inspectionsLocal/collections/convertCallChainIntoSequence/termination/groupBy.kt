// WITH_RUNTIME

fun test(list: List<Int>) {
    val groupBy = list.<caret>filter { it > 1 }.groupBy { it }
}