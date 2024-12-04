// FIR_IDENTICAL
val foo: <!UNSUPPORTED!>dynamic<!> = 1

fun foo(x: <!UNSUPPORTED!>dynamic<!>): <!UNSUPPORTED!>dynamic<!> {
    class C {
        val foo: <!UNSUPPORTED!>dynamic<!> = 1
    }
    return x + C().foo
}
