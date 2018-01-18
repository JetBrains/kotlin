// SKIP_TXT
inline fun foo(crossinline <!WRONG_MODIFIER_TARGET!>suspend<!> <!UNUSED_PARAMETER!>c<!>: () -> Unit) {}