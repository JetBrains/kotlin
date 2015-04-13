fun foo(paramTest: Int = 12)

fun test() {
    // '=' is expected
    foo(param<caret>)
}
