// WITH_RUNTIME

fun test(list: List<Int>) {
    val reduceIndexed: Int = list.<caret>filter { it > 1 }.reduceIndexed { index, acc, i -> acc + i }
}