// IS_APPLICABLE: false
fun foo(first: Int, second: Boolean, last: String) {}

fun test() {
    foo(1, true, <caret>"")
}