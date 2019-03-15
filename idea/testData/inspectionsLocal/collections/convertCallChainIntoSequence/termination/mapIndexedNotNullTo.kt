// WITH_RUNTIME

fun test(list: List<Int>) {
    val mapIndexedNotNullTo: MutableList<Int> = list.<caret>filter { it > 1 }.mapIndexedNotNullTo(mutableListOf()) { index, i -> i }
}