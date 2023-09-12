// IGNORE_BACKEND: JS_IR
// TODO: fix in KT-61881
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = h { <!INLINE_CALL_CYCLE!>f()<!> }

inline fun h(fn: ()->Unit): Unit = fn()
