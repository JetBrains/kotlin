// FIR_IDENTICAL
// !RENDER_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR
inline fun inlineFun1(crossinline p: () -> Unit) {
    object {
        fun method() { <!INLINE_CALL_CYCLE!>inlineFun2(p)<!> }
    }
}

inline fun inlineFun2(crossinline p: () -> Unit) {
    <!INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}