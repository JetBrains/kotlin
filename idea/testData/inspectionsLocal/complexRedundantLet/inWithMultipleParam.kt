// WITH_RUNTIME
// PROBLEM: none

fun foo(list: List<Int>) {
    list.filter { it.let<caret> { value -> value in value..3000 } }
}