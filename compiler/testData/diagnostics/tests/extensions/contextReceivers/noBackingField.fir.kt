interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A)
val a = 1

context(A, B)
var b = 2

context(A, B)
val c get() = <!UNRESOLVED_REFERENCE!>a<!>() + <!UNRESOLVED_REFERENCE!>b<!>()