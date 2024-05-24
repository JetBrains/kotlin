// See: KTIJ-23373
// WITH_STDLIB

fun List<Int>.forEach(action: (Int) -> Unit) {
    for (e in this) action(e)
}

fun test() {
    <expr>listOf(1, 2, 3).forEach { }</expr>
}