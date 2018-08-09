// WITH_RUNTIME

fun test(list: List<Int>) {
    val filterIsInstanceTo = list.<caret>filter { it > 1 }.filterIsInstanceTo(mutableListOf<Int>())
}