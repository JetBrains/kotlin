// WITH_RUNTIME
// PROBLEM: none

fun foo(list: List<Int>) {
    list.filter { it.let<caret> { it in IntRange(it - 1, 10) } }
}