// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo(list: List<Int>) {
    list.filter { it.let<caret> { value -> value in value..3000 } }
}