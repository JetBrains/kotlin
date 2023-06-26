// FIR_IDENTICAL
// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT

suspend inline fun inlineFun1(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE!>inlineFun2(p)<!>
}

suspend inline fun inlineFun2(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}
