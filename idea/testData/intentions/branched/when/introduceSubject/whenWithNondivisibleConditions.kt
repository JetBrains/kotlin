//IS_APPLICABLE: false
fun test(n: Int): String {
    return <caret>when {
        n in 0..10 -> "small"
        n >= 10 && n <= 100 -> "average"
        n < 0 || n > 1000 -> "unknown"
        else -> "big"
    }
}