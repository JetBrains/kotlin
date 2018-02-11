// PROBLEM: none
// WITH_RUNTIME

val map = mutableMapOf(42 to "foo")

fun foo() = map.<caret>put(60, "bar")
