fun test(n: Int): String {
    return <caret>if (n !in 0..1000)
        "unknown"
    else if (n !in 0..100)
        "big"
    else if (n !in 0..10)
        "average"
    else "small"
}