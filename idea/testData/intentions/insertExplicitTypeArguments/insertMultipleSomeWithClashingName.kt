// IS_APPLICABLE: true
class Array

fun test() {
    <caret>bar(foo(""), 0, foo(""))
}

fun foo(vararg x: String) = x

fun <T, R, V> bar(t: T, r: R, v: V) {}
