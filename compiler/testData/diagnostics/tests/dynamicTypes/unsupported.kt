// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
val foo: <!UNSUPPORTED!>dynamic<!> = 1

fun foo() {
    class C {
        val foo: <!UNSUPPORTED!>dynamic<!> = 1
    }
}
