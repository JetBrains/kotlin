// WITH_RUNTIME

fun test(list: List<Int>) {
    val associateTo: MutableMap<Int, Int> = list.<caret>filter { it > 1 }.associateTo(mutableMapOf()) { it to it }
}
