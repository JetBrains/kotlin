// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object a<!> {}
    val b = object {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object c<!> {}
    }
    b.<!UNRESOLVED_REFERENCE!>c<!>
    class A {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object d<!> {}
    }
    val f = {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object e<!> {}
    }
}
