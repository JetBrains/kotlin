package aa

val a : Int = b
val b : Int = a + b

class C {
    val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
    val b : Int = a + <!UNINITIALIZED_VARIABLE!>b<!>
}

fun foo() {
    val a : Int
    <!UNINITIALIZED_VARIABLE!>a<!> + 1
    <!UNINITIALIZED_VARIABLE!>a<!> + 1
}
