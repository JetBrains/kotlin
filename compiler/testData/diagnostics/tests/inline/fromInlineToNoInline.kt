inline fun <R> onlyLocal(p: () -> R) {
    inlineAll(<!USAGE_IS_NOT_INLINABLE!>p<!>)
}

<!NOTHING_TO_INLINE!>inline fun <R> inlineAll(noinline p: () -> R)<!> {
    p()
}