// WITH_RUNTIME

fun test(list: List<Int>) {
    val findLast: Int? = list.<caret>filter { it > 1 }.findLast { true }
}