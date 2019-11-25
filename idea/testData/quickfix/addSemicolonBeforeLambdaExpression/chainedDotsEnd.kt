// "Terminate preceding call with semicolon" "true"

fun foo() {}

fun test() {
    foo()
    {<caret>"test"}.invoke().toString().toString()
}
