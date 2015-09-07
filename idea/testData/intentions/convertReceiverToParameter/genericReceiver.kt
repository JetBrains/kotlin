// CHECK_ERRORS_AFTER

fun <T> <caret>T.bar() {
    toString()
}

fun <T> foo(a: T) {
    a.bar()
}