fun foo(a: Int, b: Int) = a + b

fun foo() {
    foo(1 <caret>+ 2, 3 * 4)
}