// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION, -UNUSED_PARAMETER

open class C<T>() {
    @Deprecated("")
    constructor(p: Int) : this(){}
}

class D : <!DEPRECATION!>C<String><!>(1)
