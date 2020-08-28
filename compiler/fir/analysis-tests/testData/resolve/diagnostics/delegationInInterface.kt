class A

interface B : <!DELEGATION_IN_INTERFACE, INTERFACE_WITH_SUPERCLASS!>A<!> by a {
    val a: A
}

val test = A()

interface C : <!DELEGATION_IN_INTERFACE, INTERFACE_WITH_SUPERCLASS!>A<!> by test