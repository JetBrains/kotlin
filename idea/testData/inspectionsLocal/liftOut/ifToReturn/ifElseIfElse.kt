fun test(n: Int): String {
    <caret>if (n == 1)
        return "one"
    else if (n == 2)
        return "two"
    else
        return "three"
}