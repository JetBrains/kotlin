// WITH_RUNTIME

fun test(list: List<Int>) {
    val elementAtOrNull: Int? = list.<caret>filter { it > 1 }.elementAtOrNull(1)
}