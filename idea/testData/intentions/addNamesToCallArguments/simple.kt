fun foo(s: String, b: Boolean){}

fun bar() {
    <caret>foo("", true)
}