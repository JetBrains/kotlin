// FIR_IDENTICAL

inline val String.foo: String
    get() = <!INLINE_CALL_CYCLE!>foo<!>
