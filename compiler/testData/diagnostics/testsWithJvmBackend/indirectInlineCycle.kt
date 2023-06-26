// FIR_IDENTICAL
// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun inlineFun1(crossinline p: () -> Unit) {
    object {
        fun method() { <!INLINE_CALL_CYCLE!>inlineFun2(p)<!> }
    }
}

inline fun inlineFun2(crossinline p: () -> Unit) {
    <!INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}
