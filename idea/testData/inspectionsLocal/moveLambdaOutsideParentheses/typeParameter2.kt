fun <T> foo(value: T, fn: () -> Unit) {}

fun test() {
    foo("", <caret>{})
}