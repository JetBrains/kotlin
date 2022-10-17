// FIR_IDENTICAL
// TARGET_BACKEND: JVM
inline val String.foo: String
    get() = <!INLINE_CALL_CYCLE!>foo<!>
