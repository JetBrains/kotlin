//IS_APPLICABLE: false
fun test(n: Int): String {
    return <caret>when(n) {
        in 0..10 -> "small"
        in 10..100 -> "average"
        else -> when {
            n in 100..1000 -> "big"
            n in 1000..10000 -> "very big"
            else -> "unknown"
        }
    }
}