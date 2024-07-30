class A

operator fun A.plus(a: A): A = A()
operator fun A.plusAssign(a: A){}

fun foo() {
    var a1 = A()
    val a2 = A()
    <!ASSIGNED_VALUE_IS_NEVER_READ!>a1<!> = a1 + a2
}
