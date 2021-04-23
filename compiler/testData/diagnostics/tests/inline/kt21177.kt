// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

class SomeContainer {
    protected class Limit

    protected fun makeLimit(): Limit = TODO()

    public inline fun foo(f: () -> Unit) {
        <!PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE!>Limit<!>()
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>makeLimit<!>()
    }
}

open class A protected constructor() {
    inline fun foo(f: () -> Unit) {
        <!PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE!>A<!>()
    }
}
