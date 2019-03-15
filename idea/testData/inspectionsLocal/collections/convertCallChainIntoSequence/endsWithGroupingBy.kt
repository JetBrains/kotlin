// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>map { it * 2 }.map { it * 3 }.map { it * 4 }.groupingBy { it }
}