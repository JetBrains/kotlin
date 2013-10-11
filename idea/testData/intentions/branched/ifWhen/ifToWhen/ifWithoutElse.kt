fun test(n: Int) {
    val s = "test"
    <caret>if (n == 0)
        s = "zero"
    else if (n == 1)
        s = "one"
    else if (n == 2)
        s = "two"
    return s
}