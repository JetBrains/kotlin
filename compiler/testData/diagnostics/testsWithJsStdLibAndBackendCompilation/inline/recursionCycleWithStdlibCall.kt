// !DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = <!INLINE_CALL_CYCLE!>run { <!INLINE_CALL_CYCLE!>f()<!> }<!>

