// IGNORE_BACKEND: JS_IR
// TODO: fix in KT-61881
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

public inline fun f():Unit = <!INLINE_CALL_CYCLE!>g()<!>

public inline fun g(): Unit = <!INLINE_CALL_CYCLE!>h()<!>

public inline fun h(): Unit = <!INLINE_CALL_CYCLE!>f()<!>
