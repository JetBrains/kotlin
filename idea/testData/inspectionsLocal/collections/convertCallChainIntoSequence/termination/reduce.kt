// WITH_RUNTIME

fun test(list: List<Int>) {
    val reduce: Int = list.<caret>filter { it > 1 }.reduce { acc, i -> acc + i }
}