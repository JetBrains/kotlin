// PROBLEM: none
fun test(n: Int): String {
    return <caret>when {
        n in 0..10 -> "n is small"
        n/10 in 0..10 -> "m is average"
        n/100 in 0..10 -> "n is big"
        else -> "unknown"
    }
}