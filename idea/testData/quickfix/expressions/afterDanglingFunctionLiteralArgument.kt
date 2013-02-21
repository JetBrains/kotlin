// "Add semicolon after invocation of 'foo'" "true"
fun foo() {}
fun foo(x : Int) {}
fun bar() {
    foo(4);

    {}<caret>
}
