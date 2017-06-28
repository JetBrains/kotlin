// "Simplify expression" "true"
// WITH_RUNTIME

fun foo(x: Int) {}

fun bar() {
    foo(<caret>if (true) {
        foo(1)
        foo(2)
        1
    } else 2)
}