// WITH_STDLIB

fun <B: Comparable<B>> myMin(xx: List<B>) {
    x<caret_1_right>x
}

fun <A: Comparable<A>> candidate(xx: List<A>) {
    x<caret_1_left>x
}
