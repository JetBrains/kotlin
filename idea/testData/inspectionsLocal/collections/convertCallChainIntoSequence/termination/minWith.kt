// WITH_RUNTIME

fun test(list: List<Int>) {
    val maxWith: Int? = list.<caret>filter { it > 1 }.maxWith(Comparator { o1, o2 -> 0 })
}