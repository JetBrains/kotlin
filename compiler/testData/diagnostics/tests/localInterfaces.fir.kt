// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!LOCAL_INTERFACE_NOT_ALLOWED!>interface a<!> {}
    val b = object {
        <!LOCAL_INTERFACE_NOT_ALLOWED!>interface c<!> {}
    }
    class A {
        <!LOCAL_INTERFACE_NOT_ALLOWED!>interface d<!> {}
    }
    val f = {
        <!LOCAL_INTERFACE_NOT_ALLOWED!>interface e<!> {}
    }
}