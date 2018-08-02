// WITH_RUNTIME

fun test(list: List<Int>) {
    val mapIndexedTo: MutableList<Int> = list.<caret>filter { it > 1 }.mapIndexedTo(mutableListOf()) { index, i -> i }
}