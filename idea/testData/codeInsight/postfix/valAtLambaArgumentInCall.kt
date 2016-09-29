// ALLOW_MULTIPLE_EXPRESSIONS
fun bar(x: (Int) -> String) = x(1)
fun foo() {
    bar() { y: Int -> "abc" }.val<caret>
}
