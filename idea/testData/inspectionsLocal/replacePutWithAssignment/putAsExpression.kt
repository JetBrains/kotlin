// PROBLEM: none
// WITH_RUNTIME

val map = mutableMapOf(42 to "foo")

fun foo() = map.put<caret>(60, "bar")
