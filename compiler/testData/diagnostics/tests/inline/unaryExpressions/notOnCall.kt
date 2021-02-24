// FIR_IDENTICAL
public inline fun test(predicate: (Char) -> Boolean) {
    !predicate('c')
}