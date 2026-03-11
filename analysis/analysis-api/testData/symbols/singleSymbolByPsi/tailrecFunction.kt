
tailrec fun fo<caret>o(i: Int): Int {
    if (i > 10) return i

    return foo(i + 1)
}