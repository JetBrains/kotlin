// "Change 'y' to '*y'" "true"

fun foo(vararg x: String) {}

fun bar(y: Array<String>) = foo(y<caret>)
