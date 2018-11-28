// WITH_RUNTIME

fun test(list: List<Int>) {
    val min: Int? = list.<caret>filter { it > 1 }.min()
}