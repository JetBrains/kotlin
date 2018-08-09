// WITH_RUNTIME

fun test(list: List<Int>) {
    val filterNotNullTo: MutableList<Int> = list.<caret>filter { it > 1 }.filterNotNullTo(mutableListOf())
}