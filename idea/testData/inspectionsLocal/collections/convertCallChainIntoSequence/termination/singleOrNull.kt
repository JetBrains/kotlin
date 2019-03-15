// WITH_RUNTIME

fun test(list: List<Int>) {
    val singleOrNull: Int? = list.<caret>filter { it > 1 }.singleOrNull()
}