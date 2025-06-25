// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = <!INLINE_CALL_CYCLE!>h()<!>

inline fun h(): Unit = <!INLINE_CALL_CYCLE!>f()<!>