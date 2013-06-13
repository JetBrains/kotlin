fun foo(n: Int) {
    <caret>var x =
        if (n > 0)
            "> 0"
        else
            "<= 0"
}