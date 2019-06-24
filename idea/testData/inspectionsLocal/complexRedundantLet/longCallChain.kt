// WITH_RUNTIME
// HIGHLIGHT: INFORMATION

fun test(list: List<Int>) {
    list.filter { it > 1 }.filter { it > 2 }.filter { it > 3 }.let<caret> { println(it) }
}