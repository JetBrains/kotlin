// !DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun f(): Unit = <!INLINE_CALL_CYCLE!>g()<!>

inline fun g(): Unit = h { <!INLINE_CALL_CYCLE!>f()<!> }

inline fun h(fn: ()->Unit): Unit = fn()