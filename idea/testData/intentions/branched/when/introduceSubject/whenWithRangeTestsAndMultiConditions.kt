fun test(n: Int): String {
    return <caret>when {
        n in 0..5, n in 5..10 -> "small"
        n in 10..50, n in 50..100 -> "average"
        n in 100..500, n in 500..1000 -> "big"
        else -> "unknown"
    }
}