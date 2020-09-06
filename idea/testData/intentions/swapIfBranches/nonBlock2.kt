fun foo() {}

fun bar() {}

fun test(b: Boolean) {
    <caret>if (b) foo()
    else bar()
}