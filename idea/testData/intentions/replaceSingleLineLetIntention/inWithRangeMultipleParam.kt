// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo(list: List<Int>) {
    list.filter { it.let<caret> { it in IntRange(it - 1, 10) } }
}