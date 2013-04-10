// "Remove useless parentheses in '(x * x / x)' expression" "true"
fun foo(x: Int) {
    x - (x * x / x)<caret> + x
}