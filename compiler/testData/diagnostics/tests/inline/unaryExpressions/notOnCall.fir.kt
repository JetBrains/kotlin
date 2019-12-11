public inline fun test(predicate: (Char) -> Boolean) {
    !predicate('c')
}