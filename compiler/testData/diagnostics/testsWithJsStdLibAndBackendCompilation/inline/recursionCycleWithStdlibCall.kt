// !DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = run { <!INLINE_CALL_CYCLE!>f()<!> }

