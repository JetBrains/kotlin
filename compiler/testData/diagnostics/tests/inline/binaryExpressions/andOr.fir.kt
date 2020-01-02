// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -TYPE_MISMATCH

inline fun inlineFunWithInvoke(s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    s && ext
    s || s
}

inline fun inlineFunWithInvokeNonInline(noinline s: (p: Int) -> Unit, ext: Int.(p: Int) -> Unit) {
    s && ext
    s || s
}

inline fun Function1<Int, Unit>.inlineExt() {
    invoke(11)
    this && this
    this || this
}