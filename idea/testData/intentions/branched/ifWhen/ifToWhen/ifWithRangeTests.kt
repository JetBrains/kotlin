fun test(n: Int): String {
    return <caret>if (n in 0..10)
        "small"
    else if (n in 10..100)
        "average"
    else if (n in 100..1000)
        "big"
    else "unknown"
}