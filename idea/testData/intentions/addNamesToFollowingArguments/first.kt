// IS_APPLICABLE: false
fun foo(first: Int, second: Boolean, last: String) {}

fun test() {
    foo(<caret>1, true, "")
}