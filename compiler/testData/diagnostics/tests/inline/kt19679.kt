inline fun test(s: () -> Unit, <!NULLABLE_INLINE_PARAMETER!>p: (() -> Unit)?<!>) {
    s()
    p?.invoke()
}