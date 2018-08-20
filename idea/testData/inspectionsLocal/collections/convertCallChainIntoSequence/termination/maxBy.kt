// WITH_RUNTIME

fun test(list: List<Int>) {
    val maxBy: Int? = list.<caret>filter { it > 1 }.maxBy { true }
}