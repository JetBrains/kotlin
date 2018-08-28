// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int>) {
    val forEach: Unit = list.<caret>filter { it > 1 }.forEach { }
}