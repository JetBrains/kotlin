// IGNORE_BACKEND: JS_IR
// TODO: fix in KT-61881
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = run { <!INLINE_CALL_CYCLE!>f()<!> }

