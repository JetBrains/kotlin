// IS_APPLICABLE: false

fun foo(n: Int, vararg s: String){}

fun bar() {
    <caret>foo(1, "")
}