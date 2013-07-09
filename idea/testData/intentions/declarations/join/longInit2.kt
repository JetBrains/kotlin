fun foo(n: Int) {
    val x: String<caret>
    x = if (n > 0)
        "> 0"
    else
        "<= 0"
}