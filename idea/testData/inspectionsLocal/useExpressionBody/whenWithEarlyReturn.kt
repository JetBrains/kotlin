// PROBLEM: none

fun sign(x: Int): Int {
    <caret>return when {
        x < 0 -> -1
        x > 0 -> if (x == 42) return 42 else 1
        else -> 0
    }
}