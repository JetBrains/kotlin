fun test(n: Int): String {
    return <caret>if (n == 0)
        "zero"
    else if (n == 1)
        "one"
    else if (n == 2)
        "two"
    else "unknown"
}