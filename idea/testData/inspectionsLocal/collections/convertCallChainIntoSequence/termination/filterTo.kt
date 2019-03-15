// WITH_RUNTIME

fun test(list: List<Int>) {
    val filterTo: MutableList<Int> = list.<caret>filter { it > 1 }.filterTo(mutableListOf()) { true }
}