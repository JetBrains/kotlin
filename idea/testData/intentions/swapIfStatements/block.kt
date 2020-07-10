fun foo() {}

fun bar() {}

fun test(i: Int) {
    <caret>if (i == 1) {
        foo()
    } else bar()
}