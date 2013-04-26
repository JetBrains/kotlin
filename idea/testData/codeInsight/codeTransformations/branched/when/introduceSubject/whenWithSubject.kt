//IS_APPLICABLE: false
fun test(n: Int): String {
    return <caret>when(n) {
        in 0..10 -> "small"
        in 10..100 -> "average"
        in 100..1000 -> "big"
        else -> "unknown"
    }
}