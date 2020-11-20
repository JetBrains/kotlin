// FIR_COMPARISON
operator fun String.get(p1: Int, p2: Int): Int = 0

fun bar() {
    ""[<caret>]
}

// ABSENT: p1
// ABSENT: p2
