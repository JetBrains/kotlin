val that: Number
    field = 239

fun test() {
    val a: Number
        <!UNRESOLVED_REFERENCE!>field<!> = 1

    val b: Number by lazy { 2 }
        <!UNRESOLVED_REFERENCE!>field<!> = 10
}

class A {
    val c: Number by lazy { 2 }
        <!BACKING_FIELD_FOR_DELEGATED_PROPERTY!>field<!> = 10
}

val A.cc: Number
    <!EXPLICIT_BACKING_FIELD_IN_EXTENSION!>field<!> = 10

fun A.cc() {
    val it = <!UNRESOLVED_REFERENCE!>a<!> + 2
}
