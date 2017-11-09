// WITH_RUNTIME

fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>true) {
        foo(1)
        foo(2)
        1
    } else 2)
}