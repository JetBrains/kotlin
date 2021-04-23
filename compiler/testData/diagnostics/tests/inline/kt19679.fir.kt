inline fun test(s: () -> Unit, p: (() -> Unit)?) {
    s()
    p?.invoke()
}
