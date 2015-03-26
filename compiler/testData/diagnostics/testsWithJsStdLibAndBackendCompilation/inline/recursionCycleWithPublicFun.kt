// !DIAGNOSTICS: -NOTHING_TO_INLINE

public inline fun f():Unit = <!INLINE_CALL_CYCLE!>g()<!>

public inline fun g(): Unit = <!INLINE_CALL_CYCLE!>h()<!>

public inline fun h(): Unit = <!INLINE_CALL_CYCLE!>f()<!>