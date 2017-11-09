fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>true) 1 else 2)
}