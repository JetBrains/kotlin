// IS_APPLICABLE: false
fun foo(s: String, b: Boolean){}

fun bar() {
    <caret>foo(s = "", b = true)
}