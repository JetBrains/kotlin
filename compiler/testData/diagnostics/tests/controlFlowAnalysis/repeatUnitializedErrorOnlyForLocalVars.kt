// IGNORE_REVERSED_RESOLVE
package aa

val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
val b : Int = a + <!UNINITIALIZED_VARIABLE!>b<!>

class C {
    val a : Int = <!UNINITIALIZED_VARIABLE!>b<!>
    val b : Int = a + <!UNINITIALIZED_VARIABLE!>b<!>
}

fun foo() {
    val a : Int
    <!UNINITIALIZED_VARIABLE!>a<!> + 1
    a + 1
}
