// "Terminate preceding call with semicolon" "true"

fun foo(
    fn: () -> Unit
) {}

fun test() {
    foo()
    {}
    {}<caret>
    {}
}
