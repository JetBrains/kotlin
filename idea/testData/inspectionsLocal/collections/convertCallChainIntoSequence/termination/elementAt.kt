// WITH_RUNTIME

fun test(list: List<Int>) {
    val elementAt = list.<caret>filter { it > 1 }.elementAt(1)
}