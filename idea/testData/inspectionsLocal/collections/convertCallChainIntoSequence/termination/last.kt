// WITH_RUNTIME

fun test(list: List<Int>) {
    val last: Int = list.<caret>filter { it > 1 }.last()
}