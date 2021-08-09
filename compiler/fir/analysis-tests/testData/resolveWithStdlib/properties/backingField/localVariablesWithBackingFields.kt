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
        field = 10
}
