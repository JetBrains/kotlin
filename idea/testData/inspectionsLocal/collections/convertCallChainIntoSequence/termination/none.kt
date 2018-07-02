// WITH_RUNTIME

fun test(list: List<Int>) {
    val none: Boolean = list.<caret>filter { it > 1 }.none()
}