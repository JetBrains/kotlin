// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
public inline fun test(predicate: (Char) -> Boolean) {
    !predicate('c')
}