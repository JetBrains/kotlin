// WITH_RUNTIME

fun test(list: List<Int>) {
    val mapNotNullTo: MutableList<Int> = list.<caret>filter { it > 1 }.mapNotNullTo(mutableListOf()) { it }
}