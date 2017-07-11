// "Delete expression" "true"

fun foo(x: Int) {}

fun bar() {
    <caret>if (false) {
        foo(1)
        foo(2)
    }
}