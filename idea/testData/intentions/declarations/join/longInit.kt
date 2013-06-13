fun foo(n: Int) {
    <caret>val x: String
    x = if (n > 0)
        "> 0"
    else
        "<= 0"
}