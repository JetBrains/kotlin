// !RENDER_DIAGNOSTICS_FULL_TEXT
// Note: 4 diagnostics per call because there are 2 synthetic $$forInline methods.
suspend inline fun inlineFun1(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE, INLINE_CALL_CYCLE, INLINE_CALL_CYCLE, INLINE_CALL_CYCLE!>inlineFun2(p)<!>
}

suspend inline fun inlineFun2(p: () -> Unit) {
    p()
    <!INLINE_CALL_CYCLE, INLINE_CALL_CYCLE, INLINE_CALL_CYCLE, INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}