// WITH_RUNTIME

fun test(list: List<Int>) {
    val associateBy: Map<Int, Int> = list.<caret>filter { it > 1 }.associateBy { it }
}