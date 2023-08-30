// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!LOCAL_INTERFACE_NOT_ALLOWED!>interface a<!> {}
    val b = object {
        <!NESTED_CLASS_NOT_ALLOWED!>interface c<!> {}
    }
    class A {
        <!NESTED_CLASS_NOT_ALLOWED!>interface d<!> {}
    }
    val f = {
        <!LOCAL_INTERFACE_NOT_ALLOWED!>interface e<!> {}
    }
}