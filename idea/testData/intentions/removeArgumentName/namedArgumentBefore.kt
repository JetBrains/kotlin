// IS_APPLICABLE: false
fun foo(s: String, b: Boolean){}

fun bar() {
    foo(s = "", <caret>b = true)
}